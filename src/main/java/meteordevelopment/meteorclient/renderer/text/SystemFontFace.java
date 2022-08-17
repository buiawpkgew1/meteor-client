package meteordevelopment.meteorclient.renderer.text;

import meteordevelopment.meteorclient.utils.render.FontUtils;

import java.io.InputStream;
import java.nio.file.Path;

public class SystemFontFace extends FontFace {
    private final Path path;

    public SystemFontFace(FontInfo info, Path path) {
        super(info);

        this.path = path;
    }

    @Override
    public InputStream toStream() {
        if (!path.toFile().exists()) {
            throw new RuntimeException("试图加载不再存在的字体.");
        }

        InputStream in = FontUtils.stream(path.toFile());
        if (in == null) throw new RuntimeException("加载字体失败 " + path + ".");
        return in;
    }

    @Override
    public String toString() {
        return super.toString() + " (" + path.toString() + ")";
    }
}
