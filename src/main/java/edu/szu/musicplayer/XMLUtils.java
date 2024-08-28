package edu.szu.musicplayer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;

public class XMLUtils {

    private XMLUtils() {
    }

    public static final XMLUtils utiles = new XMLUtils();

    public List<String> readAllGroup(String path) {

        List<String> groupList = new ArrayList<>();
        SAXReader reader = new SAXReader();

        try {
            Document dom = reader.read(getClass().getResourceAsStream(path));
            Element root = dom.getRootElement();
            if (root == null) {
                return groupList;
            }
            List<Element> groupEleList = root.elements("group");
            if (groupEleList == null || groupEleList.size() == 0) {
                return groupList;
            }
            for (Element ele : groupEleList) {
                groupList.add(ele.attributeValue("name"));
            }
        } catch (DocumentException e) {
            e.printStackTrace();
        }

        return groupList;
    }

    public Map<String, String> readGroupDetails(String path, String groupName) {
        String[] infoList = { "songList", "songIndex", "volume", "isSingleLoop", "isAllLoop", "skin", "progress" };
        Map<String, String> groupDetails = new HashMap<>();
        SAXReader reader = new SAXReader();

        try {
            Document dom = reader.read(getClass().getResourceAsStream(path));
            Element root = dom.getRootElement();
            if (root == null) {
                return groupDetails;
            }
            List<Element> groupEleList = root.elements("group");
            if (groupEleList == null || groupEleList.size() == 0) {
                return groupDetails;
            }
            for (Element ele : groupEleList) {
                if (ele.attributeValue("name").equals(groupName)) {
                    for (String key : infoList) {
                        // groupDetails.put(key, ele.elements(key).get(0).getText());
                        groupDetails.put(key, ele.elements(key).get(0).attributeValue(key));
                        System.out.println(ele.elements(key).get(0).attributeValue(key));
                    }
                    break;
                }
            }
        } catch (DocumentException e) {
            e.printStackTrace();
        }

        return groupDetails;
    }

    public void addGroupDetails(String path, String groupName, Map<String, String> groupDetails) {
        String[] infoList = { "songList", "songIndex", "volume", "isSingleLoop", "isAllLoop", "skin", "progress" };
        SAXReader reader = new SAXReader();
        try {
            Document dom = reader.read(getClass().getResourceAsStream(path));
            Element root = dom.getRootElement();
            List<Element> groupEleList = root.elements("group");
            for (Element ele : groupEleList) {
                if (ele.attributeValue("name").equals(groupName)) {
                    for (String key : infoList) {
                        Element newEle = ele.addElement(key);
                        newEle.addAttribute(key, groupDetails.get(key));
                    }
                    break;
                }
            }
            OutputFormat outputFormat = OutputFormat.createPrettyPrint();
            outputFormat.setEncoding("UTF-8");
            outputFormat.setExpandEmptyElements(true);
            XMLWriter xmlWriter = new XMLWriter(new FileWriter(new File(getClass().getResource(path).toURI())));
            xmlWriter.write(dom);
            xmlWriter.close();
        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public List<File> readAllSong(String path, String groupName) {
        List<File> songList = new ArrayList<>();
        SAXReader reader = new SAXReader();
        try {
            Document dom = reader.read(getClass().getResourceAsStream(path));
            Element root = dom.getRootElement();
            if (root == null) {
                return songList;
            }
            List<Element> groupEleList = root.elements("group");
            if (groupEleList == null || groupEleList.size() == 0) {
                return songList;
            }
            for (Element ele : groupEleList) {
                if (ele.attributeValue("name").equals(groupName)) {
                    for (Element song : ele.elements("sound")) {
                        songList.add(new File(song.element("filePath").getText()));
                    }
                    break;
                }
            }
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        return songList;
    }

    public void addGroup(String groupName, String path) {
        SAXReader reader = new SAXReader();
        try {
            Document dom = reader.read(getClass().getResourceAsStream(path));
            Element root = dom.getRootElement();
            Element groupEle = root.addElement("group");
            groupEle.addAttribute("name", groupName);

            if (!XMLUtils.utiles.readAllGroup(path).contains(groupName)) {
                OutputFormat outputFormat = OutputFormat.createPrettyPrint();
                outputFormat.setEncoding("UTF-8");
                outputFormat.setExpandEmptyElements(true);

                XMLWriter xmlWriter = new XMLWriter(
                        new FileWriter(new File(getClass().getResource(path).toURI())));

                xmlWriter.write(dom);
                xmlWriter.close();
            }
        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public void removeGroup(String groupName, String path) {
        SAXReader reader = new SAXReader();
        try {
            Document dom = reader.read(getClass().getResourceAsStream(path));
            Element root = dom.getRootElement();
            List<Element> groupEleList = root.elements("group");

            for (Element ele : groupEleList) {
                if (ele.attributeValue("name").equals(groupName)) {
                    root.remove(ele);
                    break;
                }
            }
            OutputFormat outputFormat = OutputFormat.createPrettyPrint();
            outputFormat.setEncoding("UTF-8");
            outputFormat.setExpandEmptyElements(true);
            XMLWriter xmlWriter = new XMLWriter(new FileWriter(new File(getClass().getResource(path).toURI())));

            xmlWriter.write(dom);
            xmlWriter.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addSounds(String groupName, List<File> fileList, String path) {
        SAXReader reader = new SAXReader();
        try {
            Document dom = reader.read(getClass().getResourceAsStream(path));
            Element root = dom.getRootElement();
            List<Element> groupEleList = root.elements("group");

            for (Element ele : groupEleList) {
                if (ele.attributeValue("name").equals(groupName)) {
                    for (File file : fileList) {
                        Element soundEle = ele.addElement("sound");
                        Element filePathEle = soundEle.addElement("filePath");
                        filePathEle.setText((file.getAbsolutePath()));
                    }
                    break;
                }
            }
            OutputFormat outputFormat = OutputFormat.createPrettyPrint();
            outputFormat.setEncoding("UTF-8");
            outputFormat.setExpandEmptyElements(true);
            XMLWriter xmlWriter = new XMLWriter(
                    new FileWriter(new File(getClass().getResource(path).toURI())));

            xmlWriter.write(dom);
            xmlWriter.close();

        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public void addSound(String groupName, File file, String path) {
        SAXReader reader = new SAXReader();
        try {
            Document dom = reader.read(getClass().getResourceAsStream(path));
            Element root = dom.getRootElement();
            List<Element> groupEleList = root.elements("group");

            for (Element ele : groupEleList) {
                if (ele.attributeValue("name").equals(groupName)) {
                    Element soundEle = ele.addElement("sound");
                    Element filePathEle = soundEle.addElement("filePath");
                    filePathEle.setText((file.getAbsolutePath()));
                    break;
                }
            }

            OutputFormat outputFormat = OutputFormat.createPrettyPrint();
            outputFormat.setEncoding("UTF-8");
            outputFormat.setExpandEmptyElements(true);
            XMLWriter xmlWriter = new XMLWriter(new FileWriter(new File(getClass().getResource(path).toURI())));

            xmlWriter.write(dom);
            xmlWriter.close();

        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public void removeSound(String groupName, File file, String path) {
        SAXReader reader = new SAXReader();
        String filePath = file.getAbsolutePath();
        try {
            Document dom = reader.read(getClass().getResourceAsStream(path));
            Element root = dom.getRootElement();
            List<Element> groupEleList = root.elements("group");

            for (Element ele : groupEleList) {
                if (ele.attributeValue("name").equals(groupName)) {
                    List<Element> soundEleList = ele.elements("sound");
                    for (Element soundEle : soundEleList) {
                        Element filePathEle = soundEle.element("filePath");
                        if (filePathEle.getText().equals(filePath)) {
                            ele.remove(soundEle);
                            break;
                        }
                    }
                    break;
                }
            }
            OutputFormat outputFormat = OutputFormat.createPrettyPrint();
            outputFormat.setEncoding("UTF-8");
            outputFormat.setExpandEmptyElements(true);
            XMLWriter xmlWriter = new XMLWriter(new FileWriter(new File(getClass().getResource(path).toURI())));

            xmlWriter.write(dom);
            xmlWriter.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean checkExist(String groupName, File file, String path) {
        SAXReader reader = new SAXReader();
        try {
            Document dom = reader.read(getClass().getResourceAsStream(path));
            Element root = dom.getRootElement();
            List<Element> groupEleList = root.elements("group");

            for (Element ele : groupEleList) {
                if (ele.attributeValue("name").equals(groupName)) {
                    List<Element> songList = ele.elements("song");
                    for (Element song : songList) {
                        if (song.attributeValue("filePath").equals(file.getAbsolutePath())) {
                            return true;
                        }
                    }
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
