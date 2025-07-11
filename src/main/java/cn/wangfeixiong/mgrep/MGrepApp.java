package cn.wangfeixiong.mgrep;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MGrepApp {

    public static final String RED = "\033[31m";    // 红色文本
    public static final String RESET = "\033[0m";  // 重置所有样式
    public static final String BOLD = "\033[1m";


    public static void main(String[] args){
        Set<String> keyWords = new TreeSet<>();
        String header = "";
        String currentDir = System.getProperty("user.dir");
        File file = new File(currentDir);
        String filename = "";

        if (Objects.nonNull(args) && args.length > 0) {
            String arg = String.join("v'v ", args);
            // 使用正则表达式匹配并提取参数
            Pattern pattern1 = Pattern.compile("^['\"]?(?<keywords>.+)['\"]?v'v\\s+-h\\s+['\"]?(?<header>.+)['\"]?\\s+-f +(?<filename>.+)$");
            Matcher matcher = pattern1.matcher(arg);

            Pattern pattern2 = Pattern.compile("^['\"]?(?<keywords>.+)['\"]?v'v\\s+(?<filename>.+)$");
            Matcher matcher2 = pattern2.matcher(arg);

            if (matcher.find()) {
                String keywords = matcher.group("keywords").trim();
                keyWords.addAll(Arrays.asList(keywords.split("\\s+")));
                header = matcher.group("header").trim();
                filename = matcher.group("filename").trim();
            } else if (matcher2.find()) {
                String keywords = matcher2.group("keywords").trim();
                keyWords.addAll(Arrays.asList(keywords.split("\\s+")));
                filename = matcher2.group("filename").trim();

            }
            if (filename.isEmpty()) {
                System.out.println("""
                                       使用方法：
                                        * mgrep '关键词1 关键词2'(或匹配) -h '行首的文字'  -f 文件名(支持通配符)
                                        * mgrep 关键词1 文件名(支持通配符)
                                       """);
                return;
            }
            // 用正则替换通配符
            String regex  = filename.replace(".", "\\.").replace("*", ".*");
            Pattern patternFile = Pattern.compile(regex);
            File[] files = file.listFiles((dir, name) -> patternFile.matcher(name).find());
            if (Objects.isNull(files) || files.length == 0) {
                System.out.println("当前目录没有对应的文件");
                return;
            }

            for (File file1 : files) {
                try {
                    search(file1, files.length > 1, header, keyWords);
                } catch (Exception ignored) {
                }

            }
        }

    }

    private static void search(File file, boolean multiFile, String header, Set<String> keyWords) throws IOException {
        AhoCorasickMatcher matcher = new AhoCorasickMatcher();
        for (String keyWord : keyWords) {
            matcher.addKeyword(keyWord);
        }
        matcher.buildFailurePointer();
        StringBuffer sb = new StringBuffer();
        int lineLimit = 50;
        if(header == null || header.isEmpty()) {
            lineLimit = 1;
        }else {
            String property = System.getProperty("lineLimit");
            if(Objects.nonNull(property)){
                lineLimit = Integer.parseInt(property);
            }
        }


        try (StartDelimitedLogReader reader = new StartDelimitedLogReader.Builder(
            new FileInputStream(file))
            .startPattern(header)
            .maxLinesPerBlock(lineLimit)
            .logBlockConsumer(block -> {
                List<AhoCorasickMatcher.MatchResult> allMatches = matcher.findAllMatches(block);
                if (!allMatches.isEmpty()) {
                    int start = 0;
                    for (AhoCorasickMatcher.MatchResult allMatch : allMatches) {
                        sb.append(block, start, allMatch.getStart());
                        sb.append(RED).append(BOLD).append(allMatch.getKeyword()).append(RESET);
                        start = allMatch.getEnd();
                    }
                    sb.append(block.substring(start));
                    System.out.printf((multiFile? file.getName()+" -> %s":"%s"), sb);
                    sb.setLength(0);
                }
            })
            .build()) {
            reader.processAllBlocks();
        }
    }

}
