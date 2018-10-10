package tokyo.webstudio.paraimage;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.AsyncTask;
import android.view.WindowManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class ImageLoader extends AsyncTask<ImageGroup, Integer, Boolean> {
    private static Cipher cipher;

    private IImageListener listener;
    private File imgDir;
    private Point winSize;
    private ImageGroup _group;

    ImageLoader(IImageListener listener, Activity activity) {
        this.listener = listener;
        imgDir = new File(activity.getFilesDir().getPath() + "/images");
        if (!imgDir.exists()) {
            boolean isMade = imgDir.mkdir();
            if (!isMade) {
                Util.log("failed to make directory");
            }
        }

        WindowManager wm =
                (WindowManager)activity.getSystemService(Context.WINDOW_SERVICE);
        winSize = new Point();
        wm.getDefaultDisplay().getSize(winSize);

        SecretKeySpec sKey = new SecretKeySpec(
            activity.getString(R.string.encrypt_key).getBytes(),
            "Blowfish"
        );

        try {
            cipher = Cipher.getInstance("BLOWFISH/CBC/PKCS5Padding");
            IvParameterSpec iv = new IvParameterSpec(
                activity.getString(R.string.encrypt_iv).getBytes()
            );
            cipher.init(
                Cipher.DECRYPT_MODE,
                sKey,
                iv
            );
        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException ex) {
            Util.log(ex.getMessage());
        }
    }

    private byte[] readBytes(InputStream is, int len) {
        byte[] buf = new byte[2048];
        byte[] bytes = new byte[len];

        try {
            int pos = 0;
            int readSize;
            while ((readSize = is.read(buf)) > 0) {
                System.arraycopy(
                    buf, 0, bytes, pos, readSize
                );
                pos += readSize;
            }
        } catch (IOException ex) {
            Util.log(ex.getMessage());
        }

        return bytes;
    }

    private void deleteFile(ImageGroup group) {
        if (group == null) { return; }

        File dir = new File(imgDir.getPath() + "/" + group.code);
        if (dir.exists()) {
            for (File f : dir.listFiles()) {
                if (!f.delete()) {
                    Util.log("failed to delete:" + f.getName());
                }
            }
            if (!dir.delete()) {
                Util.log("failed to delete:" + dir.getName());
            }
            Util.log("deleted:" + dir.getPath());
        }
    }

    @Override
    protected Boolean doInBackground(ImageGroup... params) {
        ImageGroup group = params[0];
        _group = group; // キャンセル処理用

        File _dir = new File(imgDir.getPath() + "/" + group.code);
        if (_dir.exists()) {
            return false;
        }

        int windowWidth = winSize.x;
        try {
            for (int i = 0; i < group.urls.length; i++) {
                if (isCancelled()) return true;

                URL url = new URL(group.urls[i] + "?w=" + windowWidth);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.setDoInput(true);
                con.connect();

                byte[] imgData = readBytes(
                    con.getInputStream(),
                    con.getContentLength()
                );

                con.disconnect();
                saveImage(group, imgData, i);

                int p = (int) ((float) i / (float) (group.urls.length) * 100f);
                listener.onProgress(p);
            }
        } catch (IOException ex) {
            Util.log(ex.getMessage());
            deleteFile(group);
        }

        return true;
    }

    private void saveImage(ImageGroup group, byte[] imgData, int imageNo) {
        if (isCancelled()) return;
        try {
            File dir = new File(imgDir.getPath() + "/" + group.code);
            if (!dir.exists()) {
                if (!dir.mkdir()) {
                    Util.log("failed to make dir:" + dir.getName());
                }
            }

            FileOutputStream io = new FileOutputStream(
                dir.getPath() + "/" + imageNo + ".jpg"
            );
            io.write(imgData, 0, imgData.length);
            io.close();
        } catch (IOException ex) {
            Util.log(ex.getMessage());
        }
    }

    Bitmap loadImage(ImageGroup group, int imageNo) throws Exception {
        byte[] decrypted = null;
        try {
            File file = new File(
                imgDir.getPath() + "/" + group.code + "/" + imageNo + ".jpg"
            );
            FileInputStream is = new FileInputStream(file);
            decrypted = cipher.doFinal(
                readBytes(is, (int) file.length())
            );
            is.close();
         } catch (IOException | BadPaddingException | IllegalBlockSizeException ex) {
            Util.log("read image error");
            Util.log(ex.getMessage());
        }

        if (decrypted == null) {
            Util.log("failed to decrypt image");
            throw new Exception();
        }

        return BitmapFactory.decodeByteArray(decrypted, 0, decrypted.length);
    }

    @Override
    protected void onPreExecute()
    {
        listener.onPreExecute();
    }

    @Override
    protected void onPostExecute(Boolean result)
    {
        listener.onPostExecute();
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        Util.log("onCancelled");
        deleteFile(_group);
    }

}
