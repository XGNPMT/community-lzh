package com.nowcoder.community.utils;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@Component
public class SensitiveFilter {
    //实例化logger
    private static final Logger logger = LoggerFactory.getLogger(SensitiveFilter.class);
    //替换符
    private static final String REPLACEMENT = "***";
    //根节点
    private TreeNode rootNode=new TreeNode();

    //初始化敏感词库
    @PostConstruct
    public void init(){
        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream("sensitive-words.txt");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is)))
        {
            String keyWord;
            while((keyWord= reader.readLine())!=null){
                this.addKeyWord(keyWord);
            }
        } catch (IOException e) {
            logger.error("加载敏感词文件失败！"+e.getMessage());
        }


    }

    private void addKeyWord(String keyWord) {
        TreeNode template = rootNode;
        for (int i = 0; i < keyWord.length(); i++) {
            Character c = keyWord.charAt(i);
            TreeNode subNode = template.getSubNode(c);
            if(subNode==null){
                subNode = new TreeNode();
                template.addSubNode(c,subNode);
            }
            template=subNode;
            //判断是否为最后一个字符
            if(i==keyWord.length()-1){
                template.setEnd(true);
            }
        }
    }
    //过滤敏感词

    /**
     *
     * @param text 待过滤文本
     * @return 过滤后的文本
     */
    public String filter(String text){
        if(StringUtils.isBlank(text)){
            return null;
        }
        StringBuilder sb = new StringBuilder();
        TreeNode tempNode=rootNode;
        int begin=0;
        int position=0;
        while(position<text.length()){
            char c = text.charAt(position);
            if(isSymbol(c)){
                if(tempNode==rootNode){
                    sb.append(c);
                    begin++;
                }
                position++;
                continue;
            }
            tempNode=tempNode.getSubNode(c);
            if(tempNode==null){
                sb.append(text.charAt(begin));
                begin++;
                position=begin;
                tempNode=rootNode;
            }else if (tempNode.isEnd()){
                sb.append(REPLACEMENT);
                begin=++position;
                tempNode=rootNode;
            }else {
                position++;
            }
        }
        sb.append(text.substring(begin));
        return sb.toString();

    }

    private boolean isSymbol(char c) {
        return !Character.isLetterOrDigit(c)&&(c<0x2E80||c>0x9FFF);
    }

    //定义前缀树的节点
    private class TreeNode{

        //是否结束
        public boolean isEnd=false;

        public boolean isEnd() {
            return isEnd;
        }

        public void setEnd(boolean end) {
            isEnd = end;
        }
        //当前节点的子节点
        private Map<Character,TreeNode> subNodes=new HashMap<>();

        //添加子节点
        public void addSubNode(Character key,TreeNode node){
            subNodes.put(key,node);
        }

        //获取子节点
        public TreeNode getSubNode(Character key){
            return subNodes.get(key);
        }


    }



}
