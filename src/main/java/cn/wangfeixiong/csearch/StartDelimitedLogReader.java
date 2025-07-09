package cn.wangfeixiong.csearch;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class StartDelimitedLogReader implements AutoCloseable {
    private final InputStream inputStream;
    private final int bufferSize;
    private final byte[] buffer;
    private int bufferPosition;
    private int bytesRead;
    private boolean isEndOfStream;

    // 用于标识下一个日志块开始的字节模式
    private final byte[] startPattern;
    // 用于处理日志块的回调函数
    private final Consumer<String> logBlockConsumer;
    // 最大行数限制
    private final int maxLinesPerBlock;
    // 换行符字节
    private static final byte NEWLINE = '\n';
    // 空模式标识
    private final boolean isEmptyPattern;

    // 构建器模式
    public static class Builder {
        private final InputStream inputStream;
        private int bufferSize = 8192;
        private byte[] startPattern;
        private Consumer<String> logBlockConsumer;
        private int maxLinesPerBlock = 20; // 默认最大行数

        public Builder(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        public Builder bufferSize(int size) {
            this.bufferSize = size;
            return this;
        }

        public Builder startPattern(String pattern) {
            this.startPattern = pattern != null ? pattern.getBytes(StandardCharsets.UTF_8) : null;
            return this;
        }

        public Builder startPattern(byte[] pattern) {
            this.startPattern = pattern;
            return this;
        }

        public Builder logBlockConsumer(Consumer<String> consumer) {
            this.logBlockConsumer = consumer;
            return this;
        }

        public Builder maxLinesPerBlock(int maxLines) {
            this.maxLinesPerBlock = maxLines;
            return this;
        }

        public StartDelimitedLogReader build() {
            if (logBlockConsumer == null) {
                throw new IllegalArgumentException("必须设置日志块消费者(logBlockConsumer)");
            }
            return new StartDelimitedLogReader(this);
        }
    }

    private StartDelimitedLogReader(Builder builder) {
        this.inputStream = new BufferedInputStream(builder.inputStream, builder.bufferSize);
        this.bufferSize = builder.bufferSize;
        this.buffer = new byte[bufferSize];
        this.bufferPosition = 0;
        this.bytesRead = 0;
        this.isEndOfStream = false;
        this.startPattern = builder.startPattern;
        this.logBlockConsumer = builder.logBlockConsumer;
        this.maxLinesPerBlock = builder.maxLinesPerBlock;
        this.isEmptyPattern = startPattern == null || startPattern.length == 0;
    }

    public void processAllBlocks() throws IOException {
        List<Byte> currentBlock = new ArrayList<>();
        int patternPosition = 0;
        int lineCount = 0;

        while (true) {
            // 填充缓冲区
            if (bufferPosition >= bytesRead) {
                bytesRead = inputStream.read(buffer);
                bufferPosition = 0;

                if (bytesRead == -1) {
                    isEndOfStream = true;
                    // 处理最后一个日志块
                    if (!currentBlock.isEmpty()) {
                        logBlockConsumer.accept(bytesToString(currentBlock));
                    }
                    break;
                }
            }

            byte currentByte = buffer[bufferPosition++];

            if (isEmptyPattern) {
                // 空模式：按换行符分割
                currentBlock.add(currentByte);
                if (currentByte == NEWLINE) {
                    lineCount++;
                }

                // 检查是否超过最大行数或到达流的末尾
                if (lineCount >= maxLinesPerBlock || (isEndOfStream && bufferPosition >= bytesRead)) {
                    logBlockConsumer.accept(bytesToString(currentBlock));
                    currentBlock.clear();
                    lineCount = 0;
                }
            } else {
                // 非空模式：按开始模式分割
                // 检查是否匹配开始模式
                if (currentByte == startPattern[patternPosition]) {
                    patternPosition++;

                    // 完整匹配开始模式
                    if (patternPosition == startPattern.length) {
                        // 处理当前日志块
                        if (!currentBlock.isEmpty()) {
                            logBlockConsumer.accept(bytesToString(currentBlock));
                            currentBlock.clear();
                            lineCount = 0;
                        }

                        // 将开始模式添加到新的日志块
                        currentBlock.addAll(Arrays.asList(bytesToBoxedArray(startPattern)));
                        patternPosition = 0;
                    }
                } else {
                    // 不匹配，重置模式位置
                    if (patternPosition > 0) {
                        // 回退并检查是否有部分匹配
                        currentBlock.addAll(Arrays.asList(bytesToBoxedArray(Arrays.copyOfRange(startPattern, 0, patternPosition))));
                        patternPosition = 0;
                    }
                    currentBlock.add(currentByte);

                    // 统计行数
                    if (currentByte == NEWLINE) {
                        lineCount++;
                    }

                    // 检查是否超过最大行数
                    if (lineCount >= maxLinesPerBlock) {
                        logBlockConsumer.accept(bytesToString(currentBlock));
                        currentBlock.clear();
                        lineCount = 0;
                    }
                }
            }
        }
    }

    private Byte[] bytesToBoxedArray(byte[] bytes) {
        Byte[] boxed = new Byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            boxed[i] = bytes[i];
        }
        return boxed;
    }

    private String bytesToString(List<Byte> bytes) {
        byte[] primitiveArray = new byte[bytes.size()];
        for (int i = 0; i < bytes.size(); i++) {
            primitiveArray[i] = bytes.get(i);
        }
        return new String(primitiveArray, StandardCharsets.UTF_8);
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }
}