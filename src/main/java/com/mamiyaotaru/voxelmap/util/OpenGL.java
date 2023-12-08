package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.GlAllocationUtils;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL30;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static com.mojang.blaze3d.platform.GlConst.*; // We're using it so much, very well might do this.

public final class OpenGL {
    // GL11
    public static final int
            GL_TRANSFORM_BIT        = 0x1000,
            GL_TEXTURE_HEIGHT       = 0x1001,
            GL_TEXTURE_BINDING_2D   = 0x8069;

    // GL12
    public static final int
            GL_UNSIGNED_INT_8_8_8_8     = 0x8035,
            GL_BGRA                     = 0x80E1,
            GL_UNSIGNED_INT_8_8_8_8_REV = 0x8367;

    // GL30
    public static final int
            GL_FRAMEBUFFER_BINDING      = 0x8CA6,
            GL_READ_FRAMEBUFFER_BINDING = 0x8CAA;

    private OpenGL() {}

    public static void generateMipmap() { GL30.glGenerateMipmap(GL_TEXTURE_2D); }

    public static final class Utils {
        private static final BufferBuilder VERTEX_BUFFER = Tessellator.getInstance().getBuffer();
        private static final IntBuffer DATA_BUFFER = GlAllocationUtils.allocateByteBuffer(16777216).asIntBuffer();

        private static int fboId = -1;
        private static int previousFboId = -1;
        private static int previousFboIdRead = -1;
        private static int previousFboIdDraw = -1;

        public static int fboTextureId = -1;

        private Utils() {}

        public static void setupFramebuffer() {
            previousFboId = GlStateManager._getInteger(GL_FRAMEBUFFER_BINDING);
            fboId = GlStateManager.glGenFramebuffers();
            fboTextureId = GlStateManager._genTexture();

            int width = 512;
            int height = 512;
            ByteBuffer buffer = BufferUtils.createByteBuffer(4 * width * height);

            GlStateManager._glBindFramebuffer(GL_FRAMEBUFFER, fboId);
            RenderSystem.bindTexture(fboTextureId);
            RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            GlStateManager._texImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_BYTE, buffer.asIntBuffer());
            GlStateManager._glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, fboTextureId, 0);

            int rboId = GlStateManager.glGenRenderbuffers();

            GlStateManager._glBindRenderbuffer(GL_RENDERBUFFER, rboId);
            GlStateManager._glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, width, height);
            GlStateManager._glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, rboId);
            GlStateManager._glBindRenderbuffer(GL_DRAW_FRAMEBUFFER, 0);

            checkFramebufferStatus();

            GlStateManager._glBindRenderbuffer(GL_DRAW_FRAMEBUFFER, previousFboId);
            GlStateManager._bindTexture(0);
        }

        public static void checkFramebufferStatus() {
            int status = GlStateManager.glCheckFramebufferStatus(GL_FRAMEBUFFER);

            if (status == GL_FRAMEBUFFER_COMPLETE) return;

            switch (status) {
                case GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT -> throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT");
                case GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT -> throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT");
                case GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER -> throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER");
                case GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER -> throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER");
                default -> throw new RuntimeException("glCheckFramebufferStatus returned unknown status: " + status);
            }
        }

        public static void bindFramebuffer() {
            previousFboId = GlStateManager._getInteger(GL_FRAMEBUFFER_BINDING);
            previousFboIdRead = GlStateManager._getInteger(GL_READ_FRAMEBUFFER_BINDING);
            previousFboIdDraw = GlStateManager._getInteger(GL_FRAMEBUFFER_BINDING);

            GlStateManager._glBindFramebuffer(GL_FRAMEBUFFER, fboId);
            GlStateManager._glBindFramebuffer(GL_READ_FRAMEBUFFER, fboId);
            GlStateManager._glBindFramebuffer(GL_DRAW_FRAMEBUFFER, fboId);
        }

        public static void unbindFramebuffer() {
            GlStateManager._glBindFramebuffer(GL_FRAMEBUFFER, previousFboId);
            GlStateManager._glBindFramebuffer(GL_READ_FRAMEBUFFER, previousFboIdRead);
            GlStateManager._glBindFramebuffer(GL_DRAW_FRAMEBUFFER, previousFboIdDraw);
        }

        public static void setMap(float x, float y, int imageSize) {
            float scale = imageSize / 4.0f;

            ldrawthree(x - scale, y + scale, 1.0, 0.0f, 1.0f);
            ldrawthree(x + scale, y + scale, 1.0, 1.0f, 1.0f);
            ldrawthree(x + scale, y - scale, 1.0, 1.0f, 0.0f);
            ldrawthree(x - scale, y - scale, 1.0, 0.0f, 0.0f);
        }

        public static void setMap(Sprite icon, float x, float y, float imageSize) {
            float half = imageSize / 4.0f;

            ldrawthree(x - half, y + half, 1.0, icon.getMinU(), icon.getMaxV());
            ldrawthree(x + half, y + half, 1.0, icon.getMaxU(), icon.getMaxV());
            ldrawthree(x + half, y - half, 1.0, icon.getMaxU(), icon.getMinV());
            ldrawthree(x - half, y - half, 1.0, icon.getMinU(), icon.getMinV());
        }

        public static int tex(BufferedImage image) {
            int glId = TextureUtil.generateTextureId();
            int width = image.getWidth();
            int height = image.getHeight();
            int[] data = new int[width * height];

            image.getRGB(0, 0, width, height, data, 0, width);
            RenderSystem.bindTexture(glId);

            DATA_BUFFER.clear();
            DATA_BUFFER.put(data, 0, width * height);
            DATA_BUFFER.position(0).limit(width * height);

            RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            RenderSystem.pixelStore(GL_UNPACK_ROW_LENGTH, 0);
            RenderSystem.pixelStore(GL_UNPACK_SKIP_PIXELS, 0);
            RenderSystem.pixelStore(GL_UNPACK_SKIP_ROWS, 0);
            GlStateManager._texImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, DATA_BUFFER);

            return glId;
        }

        public static void img(Identifier param) { VoxelConstants.getMinecraft().getTextureManager().bindTexture(param); }

        public static void register(Identifier resource, AbstractTexture image) { VoxelConstants.getMinecraft().getTextureManager().registerTexture(resource, image); }

        @NotNull
        public static NativeImage nativeImageFromBufferedImage(BufferedImage image) {
            int glId = tex(image);
            NativeImage nativeImage = new NativeImage(image.getWidth(), image.getHeight(), false);
            RenderSystem.bindTexture(glId);
            nativeImage.loadFromTextureImage(0, false);

            return nativeImage;
        }

        public static void drawPre() { VERTEX_BUFFER.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE); }

        public static void drawPost() { Tessellator.getInstance().draw(); }

        public static void ldrawone(int x, int y, double z, float u, float v) { VERTEX_BUFFER.vertex(x, y, z).texture(u, v).next(); }

        public static void ldrawthree(double x, double y, double z, float u, float v) { VERTEX_BUFFER.vertex(x, y, z).texture(u, v).next(); }
    }
}