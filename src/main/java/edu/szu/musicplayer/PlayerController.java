package edu.szu.musicplayer;

import com.leewyatt.rxcontrols.controls.RXAudioSpectrum;
import com.leewyatt.rxcontrols.controls.RXLrcView;
import com.leewyatt.rxcontrols.controls.RXMediaProgressBar;
import com.leewyatt.rxcontrols.controls.RXToggleButton;
import com.leewyatt.rxcontrols.pojo.LrcDoc;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.media.AudioSpectrumListener;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.ResourceBundle;

public class PlayerController {

    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private AnchorPane drawerPane;

    @FXML
    private BorderPane sliderPane;
    @FXML
    private StackPane soundBtn;
    @FXML
    private StackPane skinBtn;

    @FXML
    private ListView<File> listView;

    @FXML
    private RXAudioSpectrum audioSpectrum;

    @FXML
    private RXMediaProgressBar progressBar;

    @FXML
    private RXLrcView lrcView;

    @FXML
    private Label timeLabel;

    @FXML
    private ToggleButton playBtn;

    @FXML
    private StackPane likeBtn;

    private Timeline showAnim;
    private Timeline hideAnim;
    private ContextMenu soundPopup;
    private ContextMenu skinPopup;

    private MediaPlayer player;
    private Slider soundSlider;

    private SimpleDateFormat sdf = new SimpleDateFormat("mm:ss");
    private boolean isSingleLoop = false;
    private boolean isAllLoop = true;
    private Random random = new Random();
    private boolean isLoveSong = false;
    private String skinName = "black";

    /**
     * 频谱数据发生改变的时候,修改频谱可视化组件的数据
     */
    private AudioSpectrumListener audioSpectrumListener = new AudioSpectrumListener() {
        @Override
        public void spectrumDataUpdate(double timestamp, double duration, float[] magnitudes, float[] phases) {
            audioSpectrum.setMagnitudes(magnitudes);
        }
    };

    /**
     * 播放进度发生改变的时候. 修改进度条的播放进度, 修改时间的显示
     */
    private ChangeListener<Duration> durationChangeListener = (ob1, ov1, nv1) -> {
        progressBar.setCurrentTime(nv1);
        changeTimeLabel(nv1);
    };

    /**
     * 当播放时间改变的时候. 修改时间的显示
     */
    private void changeTimeLabel(Duration nv1) {
        String currentTime = sdf.format(nv1.toMillis());
        String bufferedTimer = sdf.format(player.getBufferProgressTime().toMillis());
        timeLabel.setText(currentTime + " / " + bufferedTimer);
    }

    private float[] emptyAry = new float[128];

    @FXML
    void initialize() {
        initAnim(); // 设置添加歌曲界面的动画时间
        initSoundPopup();// 设置声音滚动条控件与音量大小绑定
        initSkinPopup();// 设置目前选中的皮肤
        initListView();// 设置拖拽监听事件实现歌曲添加

        Arrays.fill(emptyAry, -60.0f);// 初始化音频可视化组件的数组为0

        initProgressBar();// 设置播放进度条控件与当前歌曲的播放进度绑定
        initPlayList();// 如果xml文件中存在group表项的name属性为lastPlay则恢复上次播放的状态
    }

    // 恢复上次播放的状态
    private void initPlayList() {
        Map<String, String> hashMap = XMLUtils.utiles.readGroupDetails("/xml/play.xml", "lastPlay");
        // hashMap.forEach((k, v) -> System.out.println(k + "-->" + v));
        if (!hashMap.isEmpty()) {
            List<File> songList = XMLUtils.utiles.readAllSong("/xml/play.xml", hashMap.get("songList"));
            // songList.forEach(e -> System.out.println(e.getAbsolutePath()));

            ObservableList<File> items = listView.getItems();
            if (songList != null) {
                songList.forEach(file -> {
                    if (!items.contains(file)) {
                        items.add(file);
                    }
                });
            }

            String volume = hashMap.get("volume");
            if (volume != null) {
                soundSlider.setValue(Double.parseDouble(volume));
            }

            String songIndex = hashMap.get("songIndex");
            if (songIndex != null) {
                listView.getSelectionModel().select(Integer.parseInt(songIndex));
            }

            String isAllLoop = hashMap.get("isAllLoop");
            if (isAllLoop != null) {
                this.isAllLoop = Boolean.parseBoolean(isAllLoop);
            }

            // String skin = hashMap.get("skin");
            // if (skin != null) {
            // String skinUrl = getClass().getResource("/css/" + skin +
            // ".css").toExternalForm();
            // String skinCommonUrl =
            // getClass().getResource("/css/common.css").toExternalForm();
            // drawerPane.getScene().getRoot().getStylesheets().setAll(skinUrl,
            // skinCommonUrl);
            // skinPopup.getScene().getRoot().getStylesheets().setAll(skinUrl,
            // skinCommonUrl);
            // soundPopup.getScene().getRoot().getStylesheets().setAll(skinUrl,
            // skinCommonUrl);
            // }

            File newFile = items.get(Integer.parseInt(songIndex));
            player = new MediaPlayer(new Media(newFile.toURI().toString()));
            player.setVolume(soundSlider.getValue() / 100);
            // 设置歌词
            String lrcPath = null;
            if (newFile.getAbsolutePath().endsWith("mp3")) {
                lrcPath = newFile.getAbsolutePath().replaceAll("mp3$", "lrc");
            } else if (newFile.getAbsolutePath().endsWith("m4a")) {
                lrcPath = newFile.getAbsolutePath().replaceAll("m4a$", "lrc");
            } else if (newFile.getAbsolutePath().endsWith("wav")) {
                lrcPath = newFile.getAbsolutePath().replaceAll("wav$", "lrc");
            } else if (newFile.getAbsolutePath().endsWith("ape")) {
                lrcPath = newFile.getAbsolutePath().replaceAll("ape$", "lrc");
            } else if (newFile.getAbsolutePath().endsWith("aiff")) {
                lrcPath = newFile.getAbsolutePath().replaceAll("aiff$", "lrc");
            }
            File lrcFile = new File(lrcPath);
            if (lrcFile.exists()) {
                try {
                    byte[] bytes = Files.readAllBytes(lrcFile.toPath());
                    // 解析歌词
                    lrcView.setLrcDoc(LrcDoc.parseLrcDoc(new String(bytes, EncodingDetect.detect(bytes))));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // 设置歌词进度
            lrcView.currentTimeProperty().bind(player.currentTimeProperty());
            // 设置频谱可视化
            player.setAudioSpectrumListener(audioSpectrumListener);
            // 设置进度条的总时长
            progressBar.durationProperty().bind(player.getMedia().durationProperty());
            // 播放器的进度修改监听器
            player.currentTimeProperty().addListener(durationChangeListener);
            // 如果播放完当前歌曲, 自动播放下一首
            player.setOnEndOfMedia(this::playNextMusic);
            playBtn.setSelected(true);

            String progress = hashMap.get("progress");
            if (progress != null && player != null) {
                player.seek(Duration.seconds(player.getTotalDuration().toSeconds() * Double.parseDouble(progress)));
            }

            String isSingleLoop = hashMap.get("isSingleLoop");
            if (isSingleLoop != null) {
                this.isSingleLoop = Boolean.parseBoolean(isSingleLoop);
                if (this.isSingleLoop) {
                    player.setOnEndOfMedia(this::playSameMusic);
                } else {
                    player.setOnEndOfMedia(this::playNextMusic);
                }
            }

            player.play();
        }
    }

    private void initProgressBar() {
        // 进度条的拖动 或者 点击 进行处理
        EventHandler<MouseEvent> progressBarHandler = event -> {
            if (player != null) {
                player.seek(progressBar.getCurrentTime());
                changeTimeLabel(progressBar.getCurrentTime());
            }
        };
        progressBar.setOnMouseClicked(progressBarHandler);
        progressBar.setOnMouseDragged(progressBarHandler);
    }

    private void initListView() {
        listView.setCellFactory(fileListView -> new MusicListCell());
        listView.getSelectionModel().selectedItemProperty().addListener((ob, oldFile, newFile) -> {
            if (player != null) {
                disposeMediaPlayer();
            }
            if (newFile != null) {
                player = new MediaPlayer(new Media(newFile.toURI().toString()));
                player.setVolume(soundSlider.getValue() / 100);
                // 设置歌词
                String lrcPath = null;
                if (newFile.getAbsolutePath().endsWith("mp3")) {
                    lrcPath = newFile.getAbsolutePath().replaceAll("mp3$", "lrc");
                } else if (newFile.getAbsolutePath().endsWith("m4a")) {
                    lrcPath = newFile.getAbsolutePath().replaceAll("m4a$", "lrc");
                } else if (newFile.getAbsolutePath().endsWith("wav")) {
                    lrcPath = newFile.getAbsolutePath().replaceAll("wav$", "lrc");
                } else if (newFile.getAbsolutePath().endsWith("ape")) {
                    lrcPath = newFile.getAbsolutePath().replaceAll("ape$", "lrc");
                }
                File lrcFile = new File(lrcPath);
                if (lrcFile.exists()) {
                    try {
                        byte[] bytes = Files.readAllBytes(lrcFile.toPath());
                        // 解析歌词
                        lrcView.setLrcDoc(LrcDoc.parseLrcDoc(new String(bytes, EncodingDetect.detect(bytes))));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                // 设置歌词进度
                lrcView.currentTimeProperty().bind(player.currentTimeProperty());
                // 设置频谱可视化
                player.setAudioSpectrumListener(audioSpectrumListener);
                // 设置进度条的总时长
                progressBar.durationProperty().bind(player.getMedia().durationProperty());
                // 播放器的进度修改监听器
                player.currentTimeProperty().addListener(durationChangeListener);
                // 如果播放完当前歌曲, 自动播放下一首
                player.setOnEndOfMedia(this::playNextMusic);
                playBtn.setSelected(true);

                player.play();
            }
        });

    }

    private void disposeMediaPlayer() {
        player.stop();
        lrcView.setLrcDoc(null);
        lrcView.currentTimeProperty().unbind();
        lrcView.setCurrentTime(Duration.ZERO);
        player.setAudioSpectrumListener(null);
        progressBar.durationProperty().unbind();
        progressBar.setCurrentTime(Duration.ZERO);
        player.currentTimeProperty().removeListener(durationChangeListener);
        audioSpectrum.setMagnitudes(emptyAry);
        timeLabel.setText("00:00 / 00:00");
        playBtn.setSelected(false);
        player.setOnEndOfMedia(null);
        player.dispose();
        player = null;
    }

    private void initSkinPopup() {
        skinPopup = new ContextMenu(new SeparatorMenuItem());
        Parent skinRoot = null;
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/skin.fxml"));
            skinRoot = fxmlLoader.load();
            ObservableMap<String, Object> namespace = fxmlLoader.getNamespace();
            ToggleGroup skinGroup = (ToggleGroup) namespace.get("skinGroup");
            skinGroup.selectedToggleProperty().addListener((ob, ov, nv) -> {
                RXToggleButton btn = (RXToggleButton) nv;
                skinName = btn.getText();
                String skinUrl = getClass().getResource("/css/" + skinName + ".css").toExternalForm();
                String skinCommonUrl = getClass().getResource("/css/common.css").toExternalForm();
                drawerPane.getScene().getRoot().getStylesheets().setAll(skinUrl, skinCommonUrl);
                skinPopup.getScene().getRoot().getStylesheets().setAll(skinUrl, skinCommonUrl);
                soundPopup.getScene().getRoot().getStylesheets().setAll(skinUrl, skinCommonUrl);
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
        skinPopup.getScene().setRoot(skinRoot);
    }

    private void initSoundPopup() {
        soundPopup = new ContextMenu(new SeparatorMenuItem());
        Parent soundRoot = null;
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/sound.fxml"));
            soundRoot = fxmlLoader.load();
            ObservableMap<String, Object> namespace = fxmlLoader.getNamespace();
            soundSlider = (Slider) namespace.get("soundSlider");
            Label soundNumLabel = (Label) namespace.get("soundNum");
            soundNumLabel.textProperty().bind(soundSlider.valueProperty().asString("%.0f%%"));
            // 声音滑块改变时,改变player的音量
            soundSlider.valueProperty().addListener((ob, ov, nv) -> {
                if (player != null) {
                    player.setVolume(nv.doubleValue() / 100);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        soundPopup.getScene().setRoot(soundRoot);
    }

    private void initAnim() {
        showAnim = new Timeline(new KeyFrame(Duration.millis(300), new KeyValue(sliderPane.translateXProperty(), 0)));
        hideAnim = new Timeline(new KeyFrame(Duration.millis(300), new KeyValue(sliderPane.translateXProperty(), 300)));
        hideAnim.setOnFinished(event -> drawerPane.setVisible(false));
    }

    @FXML
    void onHideSliderPaneAction(MouseEvent event) {
        showAnim.stop();
        hideAnim.play();
    }

    @FXML
    void onShowSliderPaneAction(MouseEvent event) {
        drawerPane.setVisible(true);
        hideAnim.stop();
        showAnim.play();
    }

    @FXML
    void onCloseAction(MouseEvent event) {
        // disposeMediaPlayer();
        XMLUtils.utiles.addGroup("recent", "/xml/play.xml");
        ObservableList<File> items = listView.getItems();
        if (items.isEmpty()) {
            Platform.exit();
        }
        List<File> fileList = new ArrayList<>();
        items.forEach(song -> fileList.add(song));
        XMLUtils.utiles.removeGroup("recent", "/xml/play.xml");
        XMLUtils.utiles.addGroup("recent", "/xml/play.xml");
        XMLUtils.utiles.addSounds("recent", fileList, "/xml/play.xml");
        // fileList.forEach(e -> System.out.println(e));
        Map<String, String> groupDetails = new HashMap<>();
        String[] infoList = { "songList", "songIndex", "volume", "isSingleLoop", "isAllLoop", "skin", "progress" };
        for (String key : infoList) {
            switch (key) {
                case "songList":
                    groupDetails.put(key, "recent");
                    break;
                case "songIndex":
                    groupDetails.put(key, String.valueOf(listView.getSelectionModel().getSelectedIndex()));
                    break;
                case "volume":
                    groupDetails.put(key, String.valueOf(player.getVolume() * 100));
                    break;
                case "isSingleLoop":
                    groupDetails.put(key, isSingleLoop ? "true" : "false");
                    break;
                case "isAllLoop":
                    groupDetails.put(key, isAllLoop ? "true" : "false");
                    break;
                case "skin":
                    groupDetails.put(key, skinName);
                    break;
                case "progress":
                    groupDetails.put(key, String.valueOf(player.getCurrentRate()));
                    break;
                default:
                    break;
            }
        }
        XMLUtils.utiles.removeGroup("lastPlay", "/xml/play.xml");
        XMLUtils.utiles.addGroup("lastPlay", "/xml/play.xml");
        XMLUtils.utiles.addGroupDetails("/xml/play.xml", "lastPlay", groupDetails);
        // status.put("")
        Platform.exit();
        // System.exit(0);
    }

    @FXML
    void onFullAction(MouseEvent event) {
        Stage stage = findStage();
        stage.setFullScreen(!stage.isFullScreen());
    }

    @FXML
    void onMiniAction(MouseEvent event) {
        Stage stage = findStage();
        stage.setIconified(true);
    }

    @FXML
    void onSoundPopupAction(MouseEvent event) {
        Bounds bounds = soundBtn.localToScreen(soundBtn.getBoundsInLocal());
        soundPopup.show(findStage(), bounds.getMinX() - 20, bounds.getMinY() - 165);
    }

    @FXML
    void onSkinPopupAction(MouseEvent event) {
        Bounds bounds = skinBtn.localToScreen(skinBtn.getBoundsInLocal());
        skinPopup.show(findStage(), bounds.getMaxX() - 135, bounds.getMaxY() + 10);
    }

    private Stage findStage() {
        return (Stage) drawerPane.getScene().getWindow();
    }

    @FXML
    void onAddMusicAction(MouseEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters()
                .add(new FileChooser.ExtensionFilter("mp3/wav/m4a/ape/flac/aiff",
                        "*.mp3", "*.flac", "*.wav", "*.m4a", "*.ape", "*.aiff"));
        List<File> fileList = fileChooser.showOpenMultipleDialog(findStage());
        XMLUtils.utiles.addSounds("favour", fileList, "/xml/play.xml");
        ObservableList<File> items = listView.getItems();
        if (fileList != null) {
            fileList.forEach(file -> {
                if (!items.contains(file)) {
                    items.add(file);
                }
            });
        }
    }

    @FXML
    void onPlayAction(ActionEvent event) {
        if (player != null) {
            if (playBtn.isSelected()) {
                player.play();
            } else {
                player.pause();
            }
        } else {
            if (listView.getItems().size() != 0) {
                listView.getSelectionModel().select(0);
            }
        }
    }

    /**
     * 播放下一首
     */
    @FXML
    void onPlayNextAction(MouseEvent event) {
        playNextMusic();
    }

    private void playNextMusic() {
        int size = listView.getItems().size();
        if (size < 2) {
            return;
        }
        int index = listView.getSelectionModel().getSelectedIndex();
        // 如果是最后一首歌, 那么下一首歌曲就是播放第一首歌曲
        if (isAllLoop) {
            index = (index == size - 1) ? 0 : index + 1;
        } else {
            index = random.nextInt(size);
        }
        listView.getSelectionModel().select(index);
        if (XMLUtils.utiles.checkExist("favour", listView.getSelectionModel().getSelectedItem(), "/xml/play.xml")) {
            likeBtn.getStylesheets().add(getClass().getResource("/css/lovesong.css").toExternalForm());
            isLoveSong = true;
        } else {
            likeBtn.getStylesheets().remove(getClass().getResource("/css/lovesong.css").toExternalForm());
            isLoveSong = false;
        }
    }

    private void playSameMusic() {
        if (player != null) {
            player.seek(Duration.ZERO);
            player.play();
        }
    }

    @FXML
    void onPlayPrevAction(MouseEvent event) {
        int size = listView.getItems().size();
        if (size < 2) {
            return;
        }
        int index = listView.getSelectionModel().getSelectedIndex();
        // 如果是最后一首歌, 那么下一首歌曲就是播放第一首歌曲
        if (isAllLoop) {
            index = (index == 0) ? size - 1 : index - 1;
        } else {
            index = random.nextInt(size);
        }
        listView.getSelectionModel().select(index);
        if (XMLUtils.utiles.checkExist("favour", listView.getSelectionModel().getSelectedItem(), "/xml/play.xml")) {
            likeBtn.getStylesheets().add(getClass().getResource("/css/lovesong.css").toExternalForm());
            isLoveSong = true;
        } else {
            likeBtn.getStylesheets().remove(getClass().getResource("/css/lovesong.css").toExternalForm());
            isLoveSong = false;
        }
    }

    @FXML
    void onAllBtnAction(ActionEvent event) {
        isAllLoop = !isAllLoop;
    }

    @FXML
    void onSingleBtnAction(ActionEvent event) {
        isSingleLoop = !isSingleLoop;
        if (!isSingleLoop) {
            player.setOnEndOfMedia(this::playNextMusic);
        } else {
            player.setOnEndOfMedia(this::playSameMusic);
        }
    }

    @FXML
    void onLikeBtnClickAction(MouseEvent event) {
        isLoveSong = !isLoveSong;
        if (isLoveSong) {
            XMLUtils.utiles.addSound("favour", listView.getSelectionModel().getSelectedItem(), "/xml/play.xml");
            likeBtn.getStylesheets().add(getClass().getResource("/css/lovesong.css").toExternalForm());
        } else {
            XMLUtils.utiles.removeSound("favour", listView.getSelectionModel().getSelectedItem(), "/xml/play.xml");
            likeBtn.getStylesheets().remove(getClass().getResource("/css/lovesong.css").toExternalForm());
        }
    }

    @FXML
    void onMVPopupAction(MouseEvent event) {
        System.out.println("视频按钮点击~~~");
    }

    @FXML
    void onSongListAction(MouseEvent event) {
        System.out.println("歌单按钮点击~~~");
    }

}
