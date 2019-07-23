package service.music;

import entity.*;
import mapper.*;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/*

*
 * @author 蒋靓峣
*/


@Service(value = "MusicVideoService")
public class MusicVideoService {
    @Resource(name = "ClassificationMapper")
    ClassificationMapper classificationMapper;
    @Resource(name = "UserMapper")
    UserMapper userMapper;
    @Resource(name = "MusicVideoMapper")
    MusicVideoMapper musicVideoMapper;
    @Resource(name = "MusicMapper")
    MusicMapper musicMapper;
    @Resource(name = "PlayMapper")
    PlayMapper playMapper;
    @Resource(name = "ShowCommentService")
    ShowCommentService showCommentService;
    @Resource(name = "PlayService")
    PlayService playService;

    /**
     *MV的详细信息
     * @param musicVideo
     * @return
     */
    public Map<String, Object> showMusicVideo(MusicVideo musicVideo) {
        Map<String, Object> musicVideoMap = new HashMap<>(10);
        List<MusicVideo> musicVideos = musicVideoMapper.selectListMusicVideo(musicVideo);
        musicVideoMap.put("musicVideo", musicVideos.get(0));
        int singerId = musicVideos.get(0).getSingerId();
        User user = new User();
        user.setId(singerId);
        musicVideoMap.put("singer", userMapper.selectUser(user).get(0));
        musicVideoMap.put("goodComment", showCommentService.commentByMusicVideoId(musicVideo.getId()));
        musicVideoMap.put("lastComment", showCommentService.commentLastByMusicVideoId(musicVideo.getId()));
        return musicVideoMap;
    }

    /**
     * 展示主页的MV
     * 排行榜前15
     * MV的id、播放量、MV的图片、name、歌手的name、歌手id
     * MV的id
     */

    public List<MusicVideoExt> exhibitionMusicVideo() {
        List<MusicVideoExt> musicVideoExts = new ArrayList<>();
        //查找出所MV的播放记录
        Play play = new Play();
        play.setType(2);
        List<Play> playList = playMapper.selectListPlay(play);
        //查找播放最多的MV对应的MV id和播放量
        Map<Integer, Integer> allMusicVideo = playService.getMostPlayMusic(playList);
        List<MusicVideo> musicVideoList =  new ArrayList<>();
        MusicVideo musicVideo = new MusicVideo();
        for(Integer musicVideoId : allMusicVideo.keySet()){
            musicVideo.setId(musicVideoId);
            musicVideoList.add(musicVideoMapper.selectListMusicVideo(musicVideo).get(0));
        }
        return transformMusicVideoExts(musicVideoList);
    }
    /**
     *根据名字查找MV
     */
    public List<MusicVideoExt> selectListMusicVideoByVideoName(MusicVideo musicVideo) {
        List<MusicVideo> musicVideoList = musicVideoMapper.selectListMusicVideo(musicVideo);
        return transformMusicVideoExts(musicVideoList);
    }

    /**
     * 通过分类查找MV
     **/

    public List<MusicVideoExt> searchMusicVideoByClassification(Classification classification) {
        List<Classification> classificationList =
                classificationMapper.selectListClassification(classification);
        List<MusicVideo> musicVideoList = new ArrayList<>();
        MusicVideo musicVideo = new MusicVideo();
        for (Classification c : classificationList) {
            musicVideo.setClassificationId(c.getId());
            musicVideoList.add(musicVideoMapper.selectListMusicVideo(musicVideo).get(0));
        }
        return transformMusicVideoExts(musicVideoList);
    }


    /**
     *  传入音乐或mv的id，获得mv或音乐的id 播放量
     * */
    public Map<Integer, Integer> musicVideoAndPlayCount(List<MusicVideo> musicVideos) {
        Map<Integer, Integer> musicCounts = new HashMap<>(16);
        for (MusicVideo musicVideo : musicVideos) {
            Play play = new Play();
            play.setMusicId(musicVideo.getMusicId());
            musicCounts.put(musicVideo.getMusicId(), playMapper.selectListPlay(play).size());
        }
        return musicCounts;
    }

    /**
     * 传入MVId返回需要显示的信息 除了播放量
     */
    public MusicVideoExt transformMusicVideoExt(int musicVideoId) {
        MusicVideoExt musicVideoExt = new MusicVideoExt();
        //传入mvId，获取相关信息
        MusicVideo temp = new MusicVideo();
        temp.setId(musicVideoId);
        MusicVideo musicVideo = musicVideoMapper.selectListMusicVideo(temp).get(0);
        musicVideoExt.setMusicVideoId(musicVideoId);
        musicVideoExt.setMusicVideoName(musicVideo.getName());
        musicVideoExt.setMusicVideoPhoto(musicVideo.getMusicVideoPhoto());
        musicVideoExt.setLevel(musicVideo.getLevel());
        musicVideoExt.setAvailable(musicVideo.getAvailable());
        //根据歌手id显示歌手名字
        User user = new User();
        user.setId(musicVideo.getSingerId());
        User singer = userMapper.selectUser(user).get(0);
        musicVideoExt.setSingerId(musicVideo.getSingerId());
        musicVideoExt.setSingerName(singer.getName());
        return musicVideoExt;
    }
    /**
     * 根据浏览量找播放量最高的mv
     */
    public  MusicVideoExt theMusicVideo(int musicId){
        return playMostVideo(musicId).get(0);
    }
    /**
     * 根据音乐id查找相关的的MV
     */
    public List<MusicVideoExt> playMostVideo(int musicId){
        MusicVideo musicVideo = new MusicVideo();
        musicVideo.setMusicId(musicId);
        List<MusicVideo> musicVideoList = musicVideoMapper.selectListMusicVideo(musicVideo);
        List<MusicVideoExt> musicVideoExts = transformMusicVideoExts(musicVideoList);
        return musicVideoExts;
    }
    /**
     * 传入一个mv集合，转成需要显示的信息,按浏览量排好序
     */
    public List<MusicVideoExt> transformMusicVideoExts(List<MusicVideo> musicVideoList){
        Map<Integer,Integer> musicVideoAndPlayCount = musicVideoAndPlayCount(musicVideoList);
        List<MusicVideoExt> musicVideoExts = new ArrayList<>();
        //排好序
        Map<Integer, Integer> rankingMusicVideo = playService.sortByValueDescending(musicVideoAndPlayCount);
        //将排好序的mvId和播放次数整理得到需要展示的信息
        for (Integer musicId : rankingMusicVideo.keySet()) {
            MusicVideoExt musicVideoExt = transformMusicVideoExt(musicId);
            musicVideoExt.setPlayCount(rankingMusicVideo.get(musicId));
            musicVideoExts.add(musicVideoExt);
        }
        return musicVideoExts;
    }
}