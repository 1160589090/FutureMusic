package service.music;

import entity.*;
import mapper.*;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 蒋靓峣 7
 */


@Service(value = "MusicVideoService")
public class MusicVideoService {
    @Resource(name = "ClassificationMapper")
    ClassificationMapper classificationMapper;
    @Resource(name = "UserMapper")
    UserMapper userMapper;
    @Resource(name = "MusicVideoMapper")
    MusicVideoMapper musicVideoMapper;
    @Resource(name = "PlayMapper")
    PlayMapper playMapper;
    @Resource(name = "PlayService")
    PlayService playService;
    @Resource(name = "CommentService")
    CommentService commentService;

    /**
     * 展示主页的MV
     * 排行榜前15
     * MV的id、播放量、MV的图片、name、歌手的name、歌手id
     * MV的id
     */
    public List<MusicVideoExt> exhibitionMusicVideo() {
        //查找出所有的MV
        List<MusicVideo> musicVideoList = musicVideoMapper.selectListMusicVideo(new MusicVideo());
        //获取对应的信息
        if(musicVideoList==null||musicVideoList.size()==0){
            return null;
        }
        List<MusicVideoExt> musicVideoExts = transformMusicVideoExts(musicVideoList);
        List<MusicVideoExt> limitMusicVideos = new ArrayList<>();
        if(musicVideoExts.size()>15) {
            for (int i = 0; i < 15; i++) {
                limitMusicVideos.add(musicVideoExts.get(i));
            }
            return limitMusicVideos;
        }
        return musicVideoExts;
    }
    /**
     *根据名字查找MV
     */
    public List<MusicVideoExt> selectListMusicVideoByVideoName(MusicVideo musicVideo) {
        List<MusicVideo> musicVideoList = musicVideoMapper.selectListMusicVideo(musicVideo);
        return transformMusicVideoExts(musicVideoList);
    }
    /**
     * 根据MusicVideo对象查找MV
     */
    public List<MusicVideoExt> selectListMusicVideo(MusicVideo musicVideo) {
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
        for (Classification c : classificationList ) {
            musicVideo.setClassificationId(c.getId());
            musicVideoList.addAll(musicVideoMapper.selectListMusicVideo(musicVideo));
        }
        return transformMusicVideoExts(musicVideoList);
    }

    /**
     *  传入音乐或mv的id，获得mv或音乐的id 播放量
     * */
    public Map<Integer, Integer> musicVideoAndPlayCount(List<MusicVideo> musicVideos) {
        Map<Integer, Integer> musicCounts = new HashMap<>(16);
        int i = 0;
        for (MusicVideo musicVideo : musicVideos) {
            Play play = new Play();
            play.setMusicId(musicVideo.getMusicId());
            play.setType(2);
            int playCount = playMapper.selectPlays(play);
            musicCounts.put(musicVideo.getMusicId(),playCount);
        }
        return musicCounts;
    }

    /**
     * 传入MVId返回需要显示的信息 包括播放量
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
        //播放量
        Play play = new Play();
        play.setMusicId(musicVideoId);
        play.setType(2);
        int playCount = playMapper.selectPlays(play);
        musicVideoExt.setPlayCount(playCount);
        return musicVideoExt;
    }
    /**
     * 根据浏览量找播放量最高的mv
     * 这个是音乐对应的MV
     */
    public  MusicVideoExt theMusicVideo(int musicId){
       List<MusicVideoExt> musicVideoList = playMostVideo(musicId);
       if(musicVideoList==null||musicVideoList.size()==0){
           return null;
       }
       MusicVideoExt musicVideo = new MusicVideoExt();
       int max = -1;
       for(MusicVideoExt mv:musicVideoList){
           if(mv.getPlayCount()>=max){
               max = mv.getPlayCount();
               musicVideo = mv;
           }
       }
       return musicVideo;
    }
    /**
     * 根据音乐id查找相关的的MV
     */
    public List<MusicVideoExt> playMostVideo(int musicId){
        MusicVideo musicVideo = new MusicVideo();
        musicVideo.setMusicId(musicId);
        List<MusicVideo> musicVideoList = musicVideoMapper.selectListMusicVideo(musicVideo);
        List<MusicVideoExt> musicVideoExts = transformMusicVideoExts(musicVideoList);
        return descendingMusicVideo(musicVideoExts);
    }
    /**
     * 传入一个mv集合，转成需要显示的信息
     */
    public List<MusicVideoExt> transformMusicVideoExts(List<MusicVideo> musicVideoList){
        List<MusicVideoExt> musicVideoExts = new ArrayList<>();
        for(MusicVideo musicVideo:musicVideoList){
            musicVideoExts.add(transformMusicVideoExt(musicVideo.getId()));
        }
        return musicVideoExts;
    }
    /**
     *按浏览量从多到少排序MV
     */
    public List<MusicVideoExt> descendingMusicVideo(List<MusicVideoExt> musicVideoExts){
        List<MusicVideoExt> descendingMusicVideo = new ArrayList<>();
        // 用冒泡排序将List集合中的元素按播放量从大到小排序
        //临时对象
        MusicVideoExt musicVideoExt;
         //遍历排序
        for (int i = 0; i < musicVideoExts.size() - 1; i++) {
            // 从后向前依次的比较相邻两个数的大小，遍历一次后，把数组中第i小的数放在第i个位置上
            for (int j =  musicVideoExts.size() - 1; j > i; j--) {
                // 比较相邻的元素，如果前面的数大于后面的数，则交换
                if (musicVideoExts.get(j - 1).getPlayCount() < musicVideoExts.get(j).getPlayCount()) {
                    musicVideoExt = musicVideoExts.get(j - 1);
                    musicVideoExts.set(j - 1,musicVideoExts.get(j));
                    musicVideoExts.set(j,musicVideoExt);
                }
            }
        }
        return descendingMusicVideo;
    }
    /**
     * 显示MV的详细信息
     */
    public List<Object> getMusicVideoInformation(int musicVideoId){
        List<Object> musicVideoInformation = new ArrayList<>();
        MusicVideo tempMusicVideo = new MusicVideo();
        tempMusicVideo.setId(musicVideoId);
        List<MusicVideo> musicVideoList = musicVideoMapper.selectListMusicVideo(tempMusicVideo);
       if(musicVideoList==null||musicVideoList.size()==0){
           return null;
       }
       MusicVideo musicVideo = musicVideoList.get(0);
        MusicVideoExt musicVideoExt = transformMusicVideoExt(musicVideo.getId());
        musicVideoInformation.add(musicVideoExt);
        musicVideoInformation.add(musicVideo.getPicture());
        List<CommentExt> commentExts = commentService.searchCommentByMusicId(musicVideoId,2);
        //评论
        musicVideoInformation.add(commentExts);
        return musicVideoInformation;
    }
}