package com.factoryonline.client.render;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowPos;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.system.MemoryUtil.NULL;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

public final class LwjglRenderHost {
    private static final int WINDOW_WIDTH = 1280;
    private static final int WINDOW_HEIGHT = 720;
    private static final String WINDOW_TITLE = "Factory Online";

    private static LwjglRenderHost instance;

    private final GLFWErrorCallback errorCallback;
    private final Thread ownerThread;
    private long windowHandle;
    private boolean shutdown;

    private LwjglRenderHost() {
        this.errorCallback = GLFWErrorCallback.createPrint(System.err);
        this.ownerThread = Thread.currentThread();
        initializeWindow();
    }

    public static synchronized LwjglRenderHost initializeSingleton() {
        if (instance != null) {
            throw new IllegalStateException("render host already initialized");
        }

        instance = new LwjglRenderHost();
        return instance;
    }

    public static synchronized LwjglRenderHost instance() {
        if (instance == null) {
            throw new IllegalStateException("render host is not initialized");
        }

        return instance;
    }

    public static synchronized void shutdownSingleton() {
        if (instance == null) {
            return;
        }

        instance.shutdown();
        instance = null;
    }

    public void runUntilClosed() {
        assertOwnerThread();

        while (!glfwWindowShouldClose(windowHandle)) {
            glfwPollEvents();
            GL11.glClearColor(0.02f, 0.02f, 0.03f, 1.0f);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            glfwSwapBuffers(windowHandle);
        }
    }

    private void initializeWindow() {
        errorCallback.set();
        if (!glfwInit()) {
            throw new IllegalStateException("unable to initialize GLFW");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        windowHandle = glfwCreateWindow(WINDOW_WIDTH, WINDOW_HEIGHT, WINDOW_TITLE, NULL, NULL);
        if (windowHandle == NULL) {
            throw new IllegalStateException("failed to create GLFW window");
        }

        glfwSetKeyCallback(windowHandle, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(window, true);
            }
        });

        centerWindow();

        glfwMakeContextCurrent(windowHandle);
        GL.createCapabilities();
        glfwSwapInterval(1);
        glfwShowWindow(windowHandle);
    }

    private void centerWindow() {
        long primaryMonitor = glfwGetPrimaryMonitor();
        if (primaryMonitor == NULL) {
            return;
        }

        GLFWVidMode videoMode = glfwGetVideoMode(primaryMonitor);
        if (videoMode == null) {
            return;
        }

        glfwSetWindowPos(
            windowHandle,
            Math.max(0, (videoMode.width() - WINDOW_WIDTH) / 2),
            Math.max(0, (videoMode.height() - WINDOW_HEIGHT) / 2));
    }

    private void shutdown() {
        assertOwnerThread();
        if (shutdown) {
            return;
        }

        shutdown = true;
        if (windowHandle != NULL) {
            glfwFreeCallbacks(windowHandle);
            glfwDestroyWindow(windowHandle);
            windowHandle = NULL;
        }

        glfwTerminate();
        GLFWErrorCallback previousCallback = glfwSetErrorCallback(null);
        if (previousCallback != null) {
            previousCallback.free();
        }
        errorCallback.free();
    }

    private void assertOwnerThread() {
        if (Thread.currentThread() != ownerThread) {
            throw new IllegalStateException("render host must be used from the owner thread");
        }
    }
}