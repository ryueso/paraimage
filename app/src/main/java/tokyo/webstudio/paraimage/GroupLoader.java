package tokyo.webstudio.paraimage;

import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class GroupLoader extends AsyncTask<String, Integer, ImageGroup[]> {
    private IGroupListener listener;
    private static final String saveFileName = "groups.properties";
    private String saveDir;

    GroupLoader(IGroupListener listener, String saveDir) {
        this.listener = listener;
        this.saveDir = saveDir;
    }

    private String readString(InputStream is, int len) {
        byte[] buf = new byte[2048];
        byte[] bytes = new byte[len];

        try {
            int pos = 0;
            int readSize;
            while ((readSize = is.read(buf)) > 0) {
                System.arraycopy(buf, 0, bytes, pos, readSize);
                pos += readSize;
            }
        } catch (IOException ex) {
            Util.log(ex.getMessage());
        }

        return new String(bytes);
    }

    @Override
    protected ImageGroup[] doInBackground(String... params) {
        ImageGroup[] groups = null;
        String jsonString = null;

        try {
            URL url = new URL(params[0]);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setDoInput(true);
            con.connect();

            InputStream in = con.getInputStream();
            jsonString = readString(
                in,
                con.getContentLength()
            );
            saveJson(jsonString);
        } catch (MalformedURLException ex) {
            Util.log(ex.getMessage());
        } catch (IOException ex) {
            Util.log(ex.getMessage());
            jsonString = readJson();
        }

        if (jsonString == null) {
            return null;
        }

        try {
            JSONArray jsonGroups = new JSONArray(jsonString);

            int len = jsonGroups.length();
            groups = new ImageGroup[len];

            for (int no = 0; no < len; no++) {
                JSONObject jsonGroup = jsonGroups.getJSONObject(no);
                JSONArray urls = jsonGroup.getJSONArray("urls");

                ImageGroup group = new ImageGroup();
                group.code = jsonGroup.getString("code");
                group.title = jsonGroup.getString("title");
                group.urls = new String[urls.length()];

                for (int imageNo = 0; imageNo < urls.length(); imageNo++) {
                    group.urls[imageNo] = urls.getString(imageNo);
                }

                groups[no] = group;

                int p = (int) ((float) no / (float) (len) * 10000f);
                if (p >= 10000) p = 9999;
                listener.onProgress(p);
            }
        } catch (JSONException ex) {
            Util.log(ex.getMessage());
        }

        return groups;
    }

    private void saveJson(String str) {
        try {
            FileOutputStream out = new FileOutputStream(
                    saveDir + "/" + saveFileName
            );
            byte[] bytes = str.getBytes();
            out.write(bytes);
            out.close();
        } catch (IOException ex) {
            Util.log(ex.getMessage());
        }
    }

    private String readJson() {
        File file = new File(
            saveDir + "/" + saveFileName
        );

        String str = null;
        if (file.exists()) {
            try {
                FileInputStream is = new FileInputStream(file);
                str = readString(is, (int) file.length());
                is.close();
            } catch (IOException ex) {
                Util.log(ex.getMessage());
            }
        }

        return str;
    }

    @Override
    protected void onPreExecute()
    {
        listener.onPreExecute();
    }

    @Override
    protected void onPostExecute(ImageGroup[] result)
    {
        listener.onPostExecute(result);
    }

}
