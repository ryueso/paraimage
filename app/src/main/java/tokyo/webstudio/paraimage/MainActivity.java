package tokyo.webstudio.paraimage;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class MainActivity extends AppCompatActivity {
    private static ImageGroup[] groups;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_PROGRESS);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_main);

        setProgressBarIndeterminate(false);
        ListView lv = findViewById(R.id.groups);
        lv.setScrollingCacheEnabled(false);
        setListener();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (groups == null) {
            GroupLoader _loader = new GroupLoader(new IGroupListener() {
                @Override
                public void onPreExecute() {
                    setProgressBarIndeterminateVisibility(true);
                    setProgressBarVisibility(true);
                    setProgress(0);
                }

                @Override
                public void onProgress(int p) {
                    setProgress(p);
                }

                @Override
                public void onPostExecute(ImageGroup[] result) {
                    setGroup(result);
                    setProgress(10000);
                    setProgressBarVisibility(false);
                    setProgressBarIndeterminateVisibility(false);
                }
            }, getFilesDir().getPath());
            _loader.execute(getString(R.string.api_url));
        } else {
            setGroup(groups);
        }
    }

    public void setGroup(ImageGroup[] g) {
        if (g == null) {
            AlertDialog.Builder b = new AlertDialog.Builder(this);
            b.setTitle(R.string.server_error_title);
            b.setMessage(R.string.server_error_msg);
            b.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            b.create();
            b.show();
            return;
        }
        groups = g;

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this, R.layout.groups
        );
        for (ImageGroup _g : groups) {
            adapter.add(_g.title);
        }

        ListView lv = findViewById(R.id.groups);
        lv.setAdapter(adapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        groups = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case R.id.close:
                finish();
                break;
            default:
                break;
        }
        return true;
    }

    private void setListener() {
        ListView lv = findViewById(R.id.groups);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(
                AdapterView<?> adapterView,
                View view,
                int pos,
                long id
            ) {
                Intent intent = new Intent(MainActivity.this, ParaActivity.class);
                intent.putExtra("group", groups[pos]);
                startActivityForResult(intent, 1);
            }
        });
    }
}
