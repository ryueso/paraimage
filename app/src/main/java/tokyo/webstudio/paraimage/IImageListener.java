package tokyo.webstudio.paraimage;

interface IImageListener {
    void onPreExecute();
    void onProgress(int p);
    void onPostExecute();
}
