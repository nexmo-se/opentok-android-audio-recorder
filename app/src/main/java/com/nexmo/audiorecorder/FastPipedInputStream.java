package com.nexmo.audiorecorder;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class FastPipedInputStream extends PipedInputStream {
    private PipedOutputStream pipedOutputStream;
    private boolean closed;

    public FastPipedInputStream(int capacity) {
        super(capacity);
        try {
            this.closed = false;
            this.pipedOutputStream = new PipedOutputStream();
            this.pipedOutputStream.connect(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized int available() throws IOException {
        if (this.closed) {
            return -1;
        }
        return super.available();
    }

    @Override
    public synchronized int read() throws IOException {
        if (this.closed) {
            return -1;
        }

        int data = super.read();
        notifyAll(); // to avoid performance issue due to sleep of 1000ms
        return data;
    }

    @Override
    public int read(byte[] b) throws IOException {
        if (this.closed) {
            return -1;
        }

        int readSize = super.read(b);
        notifyAll(); // to avoid performance issue due to sleep of 1000ms
        return readSize;
    }

    @Override
    public synchronized int read(byte[] b, int off, int len) throws IOException {
        if (this.closed) {
            return -1;
        }

        int readSize = super.read(b, off, len);
        notifyAll(); // to avoid performance issue due to sleep of 1000ms
        return readSize;
    }

    @Override
    public void close() throws IOException {
        this.closed = true;
        this.pipedOutputStream.close();
        super.close();
    }

    public void write(int b) throws IOException {
        if (this.closed) {
            return;
        }
        this.pipedOutputStream.write(b);
    }

    public void write(byte[] b) throws IOException  {
        if (this.closed) {
            return;
        }
        this.pipedOutputStream.write(b);
    }

    public void write(byte[] b, int off, int len) throws IOException  {
        if (this.closed) {
            return;
        }
        this.pipedOutputStream.write(b, off, len);
    }
}
