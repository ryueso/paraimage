package tokyo.webstudio.paraimage;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PointF;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class ParaActivity extends Activity {
    private static ImageGroup group;
    private static int currentImageNo = -1;
    private static ImageLoader loader;
    private static boolean isLoaded = false;
    private static boolean stopToast = false;

    private static final int STATUS_NONE = 0;
    private static final int STATUS_DRAG = 1;
    private static final int STATUS_ZOOM = 2;

    private int status;
    private float initialScale;
    private PointF clickPoint = new PointF();
    private Matrix clickMatrix = new Matrix();
    private float clickDistance;
    private PointF clickCenter = new PointF();
    private boolean isMoved = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.para);

        currentImageNo = -1;

        WindowManager wm =
                (WindowManager)getApplicationContext().getSystemService(WINDOW_SERVICE);
        Point size = new Point();
        wm.getDefaultDisplay().getSize(size);

        final ProgressBar progress = findViewById(R.id.progressBar);
        progress.setVisibility(ProgressBar.INVISIBLE);
        loader = new ImageLoader(new IImageListener() {
            @Override
            public void onPreExecute() {
                progress.setProgress(0);
                progress.setVisibility(ProgressBar.VISIBLE);
            }

            @Override
            public void onProgress(int p) {
                progress.setProgress(p);
            }

            @Override
            public void onPostExecute() {
                progress.setProgress(100);
                progress.setVisibility(ProgressBar.INVISIBLE);
                setLoaded(true);
                setImage(0);
            }
        }, this);

        setListener();

        Bundle extras = getIntent().getExtras();
        assert extras != null;
        loadImages((ImageGroup) extras.getSerializable("group"));
    }

    public void loadImages(ImageGroup g) {
        group = g;
        loader.execute(group);
        ((TextView) findViewById(R.id.pageNo)).setText("1");
        ((TextView) findViewById(R.id.pageTotal)).setText(String.valueOf(g.urls.length));
        setTitle(g.title);
    }

    public void setLoaded(boolean f) {
        isLoaded = f;
    }

    public void setImage(int imageNo) {
        Util.log("setImage:" + imageNo);
        if (imageNo >= group.urls.length) {
            if (!stopToast) show();
            stopToast = true;
            return;
        } else if (imageNo < 0) {
            return;
        }

        if (imageNo >= (group.urls.length - 1)) {
            Button nextBtn = findViewById(R.id.nextBtn);
            nextBtn.setEnabled(false);
        } else if (imageNo <= 0) {
            Button prevBtn = findViewById(R.id.prevBtn);
            prevBtn.setEnabled(false);
        }

        try {
            ImageView imageView = findViewById(R.id.imgView);
            Bitmap bitmap = loader.loadImage(group, imageNo);
            initialScale = (float) imageView.getWidth() / (float) bitmap.getWidth();
            Matrix matrix = new Matrix();
            matrix.setScale(initialScale, initialScale);
            imageView.setImageMatrix(matrix);
            imageView.setImageBitmap(bitmap);
            Util.log("setImageBitmap");
        } catch (Exception ex) {
            Util.log(ex.getMessage());
            return;
        } finally {
            stopToast = false;
            currentImageNo = imageNo;
        }

        ((TextView) findViewById(R.id.pageNo)).setText(String.valueOf(imageNo + 1));

        if (imageNo > 0) {
            Button prevBtn = findViewById(R.id.prevBtn);
            prevBtn.setEnabled(true);
        }
        if (imageNo < (group.urls.length - 1)) {
            Button nextBtn = findViewById(R.id.nextBtn);
            nextBtn.setEnabled(true);
        }
    }

    public void nextImage() {
        setImage(currentImageNo + 1);
    }

    public void prevImage() {
        setImage(currentImageNo - 1);
    }

    private void setListener() {
        findViewById(R.id.imgView).setOnTouchListener(
            new View.OnTouchListener() {
                @SuppressLint("ClickableViewAccessibility")
                @Override
                public boolean onTouch(View view, MotionEvent motionevent) {
                    float[] _matrixValues;
                    switch (motionevent.getAction() & MotionEvent.ACTION_MASK) {
                        case MotionEvent.ACTION_DOWN:
                            Matrix _imageMatrix = ((ImageView) view).getImageMatrix();
                            _matrixValues = new float[9];
                            _imageMatrix.getValues(_matrixValues);
                            // 拡大されている場合に限りドラッグ有効
                            if (_matrixValues[0] > initialScale) status = STATUS_DRAG;
                            clickPoint.set(motionevent.getX(), motionevent.getY());
                            clickMatrix.set(_imageMatrix);
                            break;
                        case MotionEvent.ACTION_MOVE:
                            if (status == STATUS_DRAG) {
                                Matrix _m = new Matrix();
                                _m.set(clickMatrix);
                                _m.postTranslate(
                                    motionevent.getX() - clickPoint.x,
                                    motionevent.getY() - clickPoint.y
                                );
                                ((ImageView) view).setImageMatrix(_m);
                                isMoved = true;
                            } else if (status == STATUS_ZOOM) {
                                float _scale = calcDistance(motionevent) / clickDistance;
                                Matrix _m = new Matrix();
                                _m.set(clickMatrix);
                                _m.postScale(_scale, _scale, clickCenter.x, clickCenter.y);
                                _matrixValues = new float[9];
                                _m.getValues(_matrixValues);
                                if (_matrixValues[0] <= initialScale) {
                                    // 縮小はしない
                                    return true;
                                }
                                ((ImageView) view).setImageMatrix(_m);
                                isMoved = true;
                            }
                            break;
                        case MotionEvent.ACTION_UP:
                            if (!isMoved && isLoaded) nextImage();
                            status = STATUS_NONE;
                            isMoved = false;
                            break;
                        case MotionEvent.ACTION_POINTER_DOWN:
                            status = STATUS_ZOOM;
                            clickDistance = calcDistance(motionevent);
                            if (clickDistance <= 0.0f) clickDistance = 0.1f;
                            clickCenter.set(
                                    (motionevent.getX(0) + motionevent.getX(1)) / 2.0f,
                                    (motionevent.getY(0) + motionevent.getY(1)) / 2.0f
                            );
                            break;
                        case MotionEvent.ACTION_POINTER_UP:
                            status = STATUS_NONE;
                            break;
                    }

                    return true;
                }
            }
        );

        findViewById(R.id.nextBtn).setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    nextImage();
                }
            }
        );

        findViewById(R.id.prevBtn).setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    prevImage();
                }
            }
        );

        findViewById(R.id.backBtn).setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setImage(0);
                }
            }
        );
    }

    // トーストメッセージ表示
    private void show() {
        Toast.makeText(
            this,
                R.string.return_start_pos,
            Toast.LENGTH_SHORT
        ).show();
    }

    private static float calcDistance(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float)Math.sqrt(x * x + y * y);
    }
}
