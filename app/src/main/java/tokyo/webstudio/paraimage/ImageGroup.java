package tokyo.webstudio.paraimage;

import java.io.Serializable;

public class ImageGroup implements Serializable {
    static final long serialVersionUID = 1L;

    public String code;
    public String title;
    String[] urls;
}
