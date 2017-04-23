package pl.radek.panorama.viewer;

public class TexturedMesh {
    Mesh mesh;
    int textureDataHandle = -1;

    public TexturedMesh(Mesh mesh, int textureDataHandle) {
        this.mesh = mesh;
        setTextureDataHandle(textureDataHandle);
    }

    public Mesh getMesh() {
        return mesh;
    }

    public int getTextureDataHandle() {
        return textureDataHandle;
    }

    public int setTextureDataHandle(int textureDataHandle) {
        int oldTextureDataHandle = this.textureDataHandle;
        this.textureDataHandle = textureDataHandle;
        return oldTextureDataHandle;
    }
}
