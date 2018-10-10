package tokyo.webstudio.paraimage;

interface IGroupListener {
    void onPreExecute();
    void onProgress(int p);
    void onPostExecute(ImageGroup[] result);
}
