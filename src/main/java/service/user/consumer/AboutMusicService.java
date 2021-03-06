package service.user.consumer;

import entity.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.context.ContextLoader;
import service.Emergency;
import service.user.IdExistence;
import service.user.SpecialFunctions;
import util.exception.DataBaseException;
import mapper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.apache.logging.log4j.web.WebLoggerContextUtils.getServletContext;

/**
 * 收藏音乐，收藏MV，添加历史播放记录，评论，点赞，用户播放过的音乐
 *
 * @author 5月15日 张易兴创建
 */
@Service(value = "AboutMusicService")
public class AboutMusicService {
    private static final Logger logger = LoggerFactory.getLogger(AboutMusicService.class);
    @Resource(name = "MusicCollectMapper")
    MusicCollectMapper musicCollectMapper;
    @Resource(name = "CommentMapper")
    CommentMapper commentMapper;
    @Resource(name = "PlayMapper")
    PlayMapper playMapper;
    @Resource(name = "UserMapper")
    UserMapper userMapper;
    @Resource(name = "SpecialFunctions")
    SpecialFunctions specialFunctions;
    @Resource(name = "Existence")
    ExistenceService existenceService;
    @Resource(name = "MusicMapper")
    MusicMapper musicMapper;
    @Resource(name = "MusicVideoMapper")
    MusicVideoMapper musicVideoMapper;
    @Resource(name = "OrderMapper")
    OrderMapper orderMapper;
    @Resource(name = "TransactionService")
    TransactionService transactionService;
    @Resource(name = "IdExistence")
    IdExistence idExistence;
    @Resource(name = "SongListCollectMapper")
    SongListCollectMapper songListCollectMapper;
    @Resource(name = "Emergency")
    Emergency emergency;

    /**
     * 显示用户的喜欢页面 统计喜欢歌单的个数，专辑的个数，音乐的个数，MV的个数
     */
    public String showUserLike(HttpSession session, Model model) {
        User user = specialFunctions.getUser(session);
        // 添加 歌单的个数，专辑的个数，音乐的个数，MV的个数
        getCount(session,model);
        model.addAttribute("page", "likePage");
        // 得到用户的关注粉丝量及用户信息
        specialFunctions.getUserInformation(user, model,session);
        return "userPage/userPage";
    }

    /**
     * 给当前会话上的用户，指定的model加上 歌单的个数，专辑的个数，音乐的个数，MV的个数
     */
    public void getCount(HttpSession session,Model model){
        User user = specialFunctions.getUser(session);
        // 得到用户喜欢的所有音乐
        MusicCollect musicCollect = new MusicCollect();
        musicCollect.setType(1);
        musicCollect.setUserId(user.getId());
        int music = musicCollectMapper.selectListMusicCollect(musicCollect).size();
        // 得到用户喜欢的所有MV
        musicCollect.setType(2);
        int musicVideo = musicCollectMapper.selectListMusicCollect(musicCollect).size();
        // 得到用户喜欢的所有歌单
        SongListCollect songListCollect = new SongListCollect();
        songListCollect.setType(1);
        songListCollect.setUserId(user.getId());
        // 查找到指定用户收藏的所有歌单或专辑
        int songList = songListCollectMapper.selectListSongListCollect(songListCollect).size();
        // 得到用户喜欢的所有专辑
        songListCollect.setType(2);
        int album = songListCollectMapper.selectListSongListCollect(songListCollect).size();
        model.addAttribute("musicCount", music);
        model.addAttribute("musicVideoCount", musicVideo);
        model.addAttribute("songListCount", songList);
        model.addAttribute("albumCount", album);
    }

    /**
     * 显示用户收藏的所有音乐，显示用户收藏的所有MV
     *
     * @param type 1表示查找音乐收藏 2表示查找MV收藏
     */
    public List<MusicCollect> showUserCollectionMusic(Integer type, HttpSession session) {
        //得到会话上的用户
        User user = specialFunctions.getUser(session);
        MusicCollect musicCollect = new MusicCollect();
        musicCollect.setType(type);
        musicCollect.setUserId(user.getId());
        return musicCollectMapper.selectListMusicCollect(musicCollect);
    }

    /**
     * 显示用户购买过的音乐，显示用户购买过的MV
     *
     * @param type 1表示查找音乐购买 2表示查找MV购买
     */
    public List showUserPurchaseMusic(Integer type, HttpSession session) {
        //得到会话上的用户
        User user = specialFunctions.getUser(session);
        Order order = new Order();
        order.setType(type);
        order.setUserId(user.getId());
        List<Order> list = orderMapper.selectListOrder(order);
        if (list.size() != 0) {
            List<Integer> idList = new ArrayList<>();
            // 得到所有音乐或MV的id
            for (Order o : list) {
                idList.add(o.getMusicId());
            }
            if (type == 1) {
                // 查找所有的音乐
                return musicMapper.listIdSelectListMusic(idList);
            } else {
                // 查找所有的MV
                return musicVideoMapper.listIdSelectListMusicVideo(idList);
            }
        }
        return list;
    }

    /**
     * 收藏和取消收藏音乐或MV,
     *
     * @param musicCollectInformation 封装音乐或MV收藏的信息
     *                                musicId               获取收藏音乐或MV的id
     *                                type             获取类型1是音乐2是MV
     *                                have             表示是音乐或MV是否已经购买，1表示购买，2表示没购买(不用传该数据)  判断指定用户没有有购买指定音乐或MV
     *                                singerId         表示是歌手的id
     *                                albumId          表示是专辑的id
     *                                classificationId 表示是音乐或MV的分类的id
     *                                session          获取当前会话
     */
    public State collectionMusic(MusicCollect musicCollectInformation, HttpSession session) throws DataBaseException {
        State state = new State();
        //得到会话上的用户
        User user = specialFunctions.getUser(session);
        MusicCollect musicCollect = existenceService.isUserCollectionMusic(user.getId(), musicCollectInformation.getMusicId(), musicCollectInformation.getType());
        if (musicCollect != null) {
            // 如果不为null表示已经收藏则需要用取消收藏
            if (musicCollectMapper.deleteMusicCollect(musicCollect.getId()) < 1) {
                // 如果失败是数据库错误
                logger.error("邮箱：" + user.getMailbox() + "删除收藏的音乐或MV时，数据库出错");
                throw new DataBaseException("邮箱：" + user.getMailbox() + "删除收藏的音乐或MV时，数据库出错");
            }
            //删除收藏成功
            state.setState(1);
        } else {
            // 为null表示没有收藏，需要添加收藏
            int have = 0;
            // 判断用户是否购买了该MV或音乐
            Order order = transactionService.isPurchaseMusic(musicCollectInformation.getMusicId(), musicCollectInformation.getType(), user);
            if (order != null) {
                have = 1;
            }
            musicCollectInformation.setUserId(user.getId());
            // 添加收藏
            collectionAndPlay(have, musicCollectInformation, null, user);
            state.setState(2);
        }
        return state;
    }

    /**
     * 音乐或MV的历史播放记录
     *
     * @param playInformation 封装评论的信息
     *                        musicId               获取收藏音乐或MV的id
     *                        type             获取类型1是音乐2是MV
     *                        singerId         表示是歌手的id
     *                        albumId          表示是专辑的id
     *                        classificationId 表示是音乐或MV的分类的id
     * @param session         获取当前会话
     */
    public State musicPlay(Play playInformation, HttpSession session) throws DataBaseException {
        //得到会话上的用户
        User user = specialFunctions.getUser(session);
        Play play = existenceService.isUserMusicPlay(user.getId(), playInformation.getMusicId(), playInformation.getType());
        // 先判断该用户是否听过该音乐或MV，如果听过只需要更新时间
        if (play != null) {
            // 如果不为null表示已经播放过
            play.setDate(new Date());
            if (playMapper.updatePlay(play) < 1) {
                // 如果失败是数据库错误
                logger.error("邮箱：" + user.getMailbox() + "更新音乐或MV的播放记录时，数据库出错");
                throw new DataBaseException("邮箱：" + user.getMailbox() + "更新音乐或MV的播放记录时，数据库出错");
            }
        } else {
            // 没有播放过需要添加播放记录
            collectionAndPlay(0, null, playInformation, user);
        }
        return new State(1);
    }

    /**
     * 为指定的音乐或MV，添加收藏或播放记录
     *
     * @param have         只有收藏有
     * @param musicCollect 音乐或MV收藏信息
     * @param play         音乐或MV播放信息
     * @param user         用户信息
     */
    private void collectionAndPlay(int have, MusicCollect musicCollect, Play play, User user) throws DataBaseException {
        // 该音乐或MV的歌手
        int singerId;
        // 该音乐的专辑 mv没有
        int albumId = 0;
        // 该音乐或MV的分类
        int classificationId;
        // 存储类型1表示音乐 2表示MV
        int type;
        // 音乐或MV的id
        int musicId;
        if (musicCollect != null) {
            type = musicCollect.getType();
            musicId = musicCollect.getMusicId();
        } else {
            type = play.getType();
            musicId = play.getMusicId();
        }
        // 表示音乐，去音乐表查询
        if (type == 1) {
            Music music = new Music();
            music.setId(musicId);
            // 查找指定id信息
            music = musicMapper.selectListMusic(music).get(0);
            singerId = music.getSingerId();
            albumId = music.getAlbumId();
            classificationId = music.getClassificationId();
        } else {
            //表示MV，去MV表查找
            MusicVideo musicVideo = new MusicVideo();
            musicVideo.setId(musicId);
            // 查找指定id信息
            musicVideo = musicVideoMapper.selectListMusicVideo(musicVideo).get(0);
            singerId = musicVideo.getSingerId();
            classificationId = musicVideo.getClassificationId();
        }
        if (musicCollect != null) {
            musicCollect.setUserId(user.getId());
            musicCollect.setMusicId(musicId);
            musicCollect.setHave(have);
            musicCollect.setType(type);
            musicCollect.setSingerId(singerId);
            musicCollect.setAlbumId(albumId);
            musicCollect.setClassificationId(classificationId);
            musicCollect.setDate(new Date());
            if (musicCollectMapper.insertMusicCollect(musicCollect) < 1) {
                // 如果失败是数据库错误
                logger.error("邮箱：" + user.getMailbox() + "收藏音乐或MV时，数据库出错");
                throw new DataBaseException("邮箱：" + user.getMailbox() + "收藏音乐或MV时，数据库出错");
            }
        } else {
            play.setUserId(user.getId());
            play.setMusicId(musicId);
            play.setType(type);
            play.setSingerId(singerId);
            play.setAlbumId(albumId);
            play.setClassificationId(classificationId);
            play.setDate(new Date());
            // 添加该音乐或MV的播放记录
            if (playMapper.insertPlay(play) < 1) {
                // 如果失败是数据库错误
                logger.debug("邮箱：" + user.getMailbox() + "添加音乐或MV的播放记录时，数据库出错");
                throw new DataBaseException("邮箱：" + user.getMailbox() + "添加音乐或MV的播放记录时，数据库出错");
            }
        }
    }

    /**
     * 显示用户的音乐或MV的历史播放记录,ajax
     *
     * @param type    1表示音乐 2表示MV
     * @param session 获取当前会话
     */
    public List<?> showMusicPlay(Integer type, HttpSession session) {
        //得到会话上的用户
        User user = specialFunctions.getUser(session);
        Play play = new Play();
        play.setType(type);
        play.setUserId(user.getId());
        // 查找用户播放过的音乐或MV
        List<Play> list = playMapper.selectListPlay(play);
        // 用来存放音乐或MV的id
        List<Integer> idList = new ArrayList<>();
        for (Play p : list) {
            idList.add(p.getMusicId());
        }
        if (type == 1) {
            // 1表示音乐
            return musicMapper.listIdSelectListMusic(idList);
        } else {
            // 2表示MV
            return musicMapper.listIdSelectListMusic(idList);
        }
    }

    /**
     * 音乐或MV或专辑的评论，ajax
     *
     * @param commentInformation 封装平路的信息
     *                           获取音乐或MV或专辑的id
     *                           获取类型1是音乐，2是MV，3是专辑
     *                           获取评论的内容
     *                           获取回复哪个评论的id, 0为独立评论
     * @param session            获取当前会话
     */
    public State comment(Comment commentInformation, HttpSession session) throws DataBaseException {
        State state = new State();
        if (commentInformation.getContent().length() != 0) {
            if (commentInformation.getContent().length() <= 200) {
                //得到会话上的用户
                User user = specialFunctions.getUser(session);
                Comment comment = new Comment();
                comment.setMusicId(commentInformation.getMusicId());
                comment.setType(commentInformation.getType());
                comment.setUserId(user.getId());
                comment.setContent(commentInformation.getContent());
                comment.setDate(new Date());
                comment.setReply(commentInformation.getReply());
                if (commentMapper.insertComment(comment) < 1) {
                    // 如果失败是数据库错误
                    logger.error("邮箱：" + user.getMailbox() + "评论时，数据库出错");
                    throw new DataBaseException("邮箱：" + user.getMailbox() + "评论时，数据库出错");
                }
                state.setState(1);
            } else {
                state.setInformation("内容过长");
            }
        } else {
            state.setInformation("内容不能为空");
        }
        return state;
    }

    /**
     * 删除指定评论及他的子评论
     *
     * @param id 获取要删除评论的id
     */
    public State deleteComment(Integer id) throws DataBaseException {
        State state = new State();
        // 判断是否有子评论，没有返回null
        int[] replyId = existenceService.isComment(id);
        // 先将自己删除
        if (commentMapper.deleteComment(id) < 1) {
            // 如果失败是数据库错误
            logger.error("删除评论时，数据库出错");
            throw new DataBaseException("删除评论时，数据库出错");
        }
        // 如果有子评论继续删除
        if (replyId != null) {
            // 递归删除该评论的所有子评论
            for (int i : replyId) {
                deleteComment(i);
            }
        }
        state.setState(1);
        return state;
    }


    /**
     * 评论点赞或取消点赞（更改用户的点赞记录，更改评论被点赞的次数），事务处理
     *
     * @param id      评论的id
     * @param session 获取当前会话
     */
    @Transactional
    public synchronized State commentFabulous(String id, HttpSession session) throws DataBaseException {
        //得到会话上的用户
        User user = specialFunctions.getUser(session);
        // 得到用户给哪些评论点过赞
        StringBuilder fabulousString = new StringBuilder(user.getFabulous());
        String[] fabulous = fabulousString.toString().split("#");
        boolean isFabulous = true;
        // 判断用户是否给该评论点过赞，如果点过则取消
        for (String s : fabulous) {
            if (s.equals(id)) {
                // 该评论点过赞，更改用户的点赞记录，并减少该评论点赞次数
                // 初始化
                fabulousString = new StringBuilder();
                // 先添加第一个,因为先添加第一个，所以变量从第二个开始循环
                int index = 1;
                if (!fabulous[0].equals(id)) {
                    fabulousString.append(fabulous[0]);
                } else {
                    //第一个是删除的id，第二个成为第一个（开头不需要加#）
                    fabulousString.append(fabulous[1]);
                    // 所以变量从第三个开始循环
                    index = 2;
                }
                for (; index < fabulous.length; index++) {
                    if (!fabulous[index].equals(id)) {
                        // 进行字符串拼接
                        fabulousString.append("#").append(fabulous[index]);
                    }
                }
                user.setFabulous(fabulousString.toString());
                isFabulous = false;
                break;
            }
        }
        Comment comment = new Comment();
        comment.setId(Integer.parseInt(id));
        List<Comment> commentList = commentMapper.selectListComment(comment);
        // 得带查找到的评论
        comment = commentList.get(0);
        if (isFabulous) {
            // 表示没有给该评论点过赞, 则将该评论的id加到后面
            user.setFabulous(user.getFabulous() + "#" + id);
            // 增加评论的点赞次数
            comment.setFabulous(comment.getFabulous() + 1);
        } else {
            // 减少评论的点赞次数
            comment.setFabulous(comment.getFabulous() - 1);
        }
        //事务处理
        // 更新评论的点赞次数
        if (commentMapper.updateComment(comment) < 1) {
            // 如果失败是数据库错误
            logger.error("邮箱：" + user.getMailbox() + "修改用户点赞记录时，数据库出错");
            throw new DataBaseException("邮箱：" + user.getMailbox() + "修改用户点赞记录时，数据库出错");
        }
        // 更新用户的数据库
        if (userMapper.updateUser(user) < 1) {
            // 如果失败是数据库错误
            logger.error("邮箱：" + user.getMailbox() + "修改用户点赞记录时，数据库出错");
            throw new DataBaseException("邮箱：" + user.getMailbox() + "修改用户点赞记录时，数据库出错");
        }
        return new State(1);
    }


    /**
     * 播放音乐判断用户是否拥有该音乐
     *
     * @param id 音乐的id   id为0表示没版权  为1表示没有VIP 为2表示没购买  为3表示音乐无歌词
     */
    public Music playMusic(Integer id, HttpSession session) {
        //得到会话上的用户
        User user = specialFunctions.getUser(session);
        user=idExistence.isUserId(user.getId());
        // 查找指定的音乐信息
        Music music = idExistence.isMusicId(id);
        // 得到音乐的等级
        int level = music.getLevel();
        // 先判断该音乐有没有版权 0表示有版权
        System.out.println(music.getAvailable());
        if (music.getAvailable() == 0) {
            if (level == 2) {
                System.out.println(user.getVipDate());
                // 判断用户是不是VIP
                if (user.getVipDate().getTime() < System.currentTimeMillis()) {
                    music.setId(1);
                }
            } else if (level == 3) {
                // 判断用户有没有购买，不为null表示购买
                if (transactionService.isPurchaseMusic(id, 1,user) == null) {
                    music.setId(2);
                }
            }
            // 等级为1 ，表示免费
        } else {
            music.setId(0);
        }
        // 使用io流读取指定音乐的歌词
        InputStream inputStream = null;
        try {
            System.out.println(music.getLyricPath());
            if (music.getLyricPath() == null || "".equals(music.getLyricPath())) {
                music.setLyricPath("/static");
            }
            System.out.println(ContextLoader.getCurrentWebApplicationContext());
            System.out.println(ContextLoader.getCurrentWebApplicationContext().getServletContext().getRealPath(""));
            inputStream = new FileInputStream(ContextLoader.getCurrentWebApplicationContext().getServletContext().getRealPath(music.getLyricPath()));
//            inputStream = new FileInputStream("C:\\first\\FutureMusic\\target\\FutureMusic\\"+);
            // 先使用反射获取项目文件的路径，然后获得缓冲流
            BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
            byte[] bytes = new byte[1024];
            // 一次读取的长度
            int length;
            StringBuilder lyric = new StringBuilder();
            while ((length = bufferedInputStream.read(bytes)) != -1) {
                lyric.append(new String(bytes, 0, length));
            }
            music.setLyricPath(new String(lyric));
        } catch (Exception e) {
            music.setId(3);
            e.printStackTrace();
        } finally {
            try {
                assert inputStream != null;
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        logger.debug("音乐的信息为：" + music.getId());
        return music;
    }


    /**
     * 播放MV判断用户是否购买该MV
     *
     * @param id MV的id id为0表示没版权  为1表示没有VIP 为2表示没购买
     */
    public MusicVideo playMusicVideo(Integer id, HttpSession session) {
        //得到会话上的用户
        User user = specialFunctions.getUser(session);
        user=idExistence.isUserId(user.getId());
        // 查找指定的音乐信息
        MusicVideo musicVideo = idExistence.isMusicVideoId(id);
        // 得到MV的等级
        int level = musicVideo.getLevel();
        // 先判断该MV有没有版权 0表示有版权
        if (musicVideo.getAvailable() == 0) {
            if (level == 2) {
                // 判断用户是不是VIP
                if (user.getVipDate().getTime() < System.currentTimeMillis()) {
                    musicVideo.setId(1);
                }
            } else if (level == 3) {
                // 判断用户有没有购买，不为null表示购买
                if (transactionService.isPurchaseMusic(id, 2, user) == null) {
                    musicVideo.setId(2);
                }
            }
            // 等级为1 ，表示免费
        } else {
            musicVideo.setId(0);
        }
        List<MusicVideo> list = new ArrayList<>();
        list.add(musicVideo);
        logger.debug("播放的MV"+musicVideo);
        return emergency.getMusicVideoCollect(user, list).get(0);
    }
}