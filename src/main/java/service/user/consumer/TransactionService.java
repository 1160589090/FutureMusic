package service.user.consumer;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import entity.*;
import mapper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import service.user.IdExistence;
import service.user.SpecialFunctions;
import service.user.administrators.UserInformationService;
import util.AlipayConfig;
import util.exception.DataBaseException;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.*;

/**
 * 支付与购买
 *
 * @author HP
 */
@Service(value = "TransactionService")
public class TransactionService {
    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);
    @Resource(name = "SpecialFunctions")
    SpecialFunctions specialFunctions;
    @Resource(name = "OrderMapper")
    OrderMapper orderMapper;
    @Resource(name = "UserInformationService")
    UserInformationService userInformationService;
    @Resource(name = "IdExistence")
    IdExistence idExistence;
    @Resource(name = "Existence")
    ExistenceService existenceService;
    @Resource(name = "MusicCollectMapper")
    MusicCollectMapper musicCollectMapper;
    @Resource(name = "AboutSongListService")
    AboutSongListService aboutSongListService;
    @Resource(name = "MusicSongListMapper")
    MusicSongListMapper musicSongListMapper;
    @Resource(name = "UserMapper")
    UserMapper userMapper;


    /**
     * 充值完回调此方法
     */
    public synchronized String rechargeBalance(HttpServletRequest request, HttpSession session) throws UnsupportedEncodingException, AlipayApiException, DataBaseException {
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (Iterator<String> iter = requestParams.keySet().iterator(); iter.hasNext(); ) {
            String name = (String) iter.next();
            String[] values = (String[]) requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i]
                        : valueStr + values[i] + ",";
            }
            //乱码解决，这段代码在出现乱码时使用
            valueStr = new String(valueStr.getBytes("ISO-8859-1"), "utf-8");
            params.put(name, valueStr);
        }
        //调用SDK验证签名
        boolean signVerified = AlipaySignature.rsaCheckV1(params, AlipayConfig.alipay_public_key, AlipayConfig.charset, AlipayConfig.sign_type);
        if (signVerified) {
            String money = request.getParameter("total_amount");
            String token = (String) session.getAttribute("token");
            logger.debug("token的值" + token);
            logger.debug("money的值" + money);
            logger.debug("" + money.equals(token));
            if (money.equals(token)) {
                session.removeAttribute("token");
                // 获取充值金额
                User user = specialFunctions.getUser(session);
                logger.debug("充值前的余额" + user.getBalance());
                String originalPrice=money;
                if ("50.00".equals(money)) {
                    money = "60.00";
                } else if ("100.00".equals(money)) {
                    money = "130.00";
                }
                user.setBalance(user.getBalance().add(new BigDecimal(money)));
                logger.debug("充值后的余额" + user.getBalance());
                // 添加订单信息,失败抛异常
                getOrder(4,user.getId(),0,new BigDecimal(originalPrice),new BigDecimal(money));
                // 该方法更改了需要更改相应的参数
                userInformationService.modifyUser(String.valueOf(user.getId()), String.valueOf(user.getLevel()), String.valueOf(user.getBalance()), String.valueOf(user.getReport()));
                return "index";
            }
        } else {//验证失败
            logger.debug("验证失败");
            State state = new State();
            state.setInformation("支付失败");
        }
        logger.debug("抛异常");
        throw new AlipayApiException();
    }

    /**
     * 购买音乐或MV (需要绑定事务)
     *
     * @param id   得到购买的id
     * @param type 得到购买的类型 1表示音乐  2表示MV
     */
    @Transactional
    public State purchase(Integer id, Integer type, HttpSession session) throws DataBaseException {
        State state = new State();
        User user = specialFunctions.getUser(session);
        user = idExistence.isUserId(user.getId());
        // 首先判断用户是不是已经购买了指定音乐过MV
        if (isPurchaseMusic(id, type, user) == null) {
            // 判断音乐或MV是否有版权，
            // 用来存储音乐和MV所属于的歌手
            int singerId;
            // 用来存储音乐所属于的专辑,MV没有专辑
            int albumId = 0;
            // 用来存储音乐和MV所属于的分类
            int classificationId;
            //用来存储音乐或MV的原价格
            BigDecimal originalPrice;
            // 用来存储活动的id
            int activityId;
            if (type == 1) {
                // 获得音乐的信息
                Music music = idExistence.isMusicId(id);
                // 判断该音乐有没有版权
                if (music == null || music.getAvailable() == 1) {
                    logger.debug("音乐没有版权");
                    state.setInformation("该音乐没有版权");
                    return state;
                }
                originalPrice = music.getPrice();
                activityId = music.getActivity();
                singerId = music.getSingerId();
                classificationId = music.getClassificationId();
                albumId = music.getAlbumId();
            } else {
                // 获得MV的信息
                MusicVideo musicVideo = idExistence.isMusicVideoId(id);
                if (musicVideo == null || musicVideo.getAvailable() == 1) {
                    logger.debug("MV没有版权");
                    state.setInformation("该MV没有版权");
                    return state;
                }
                originalPrice = musicVideo.getPrice();
                activityId = musicVideo.getActivity();
                singerId = musicVideo.getSingerId();
                classificationId = musicVideo.getClassificationId();
            }
            // 判断购买的商品是否参加了活动
            // 用来存储活动的折扣
            float discount = getDiscount(activityId);
            // 得到打折后的价格，然后对价格保留两位小数进行进位处理，得到最后的音乐的价格
            BigDecimal price = originalPrice.multiply(BigDecimal.valueOf(discount)).setScale(2, BigDecimal.ROUND_UP);
            // 得到用户是否买的起
            logger.debug("打折后的价格" + price);
            logger.debug("价格对比-1小于、0等于 或 1大于" + price.compareTo(user.getBalance()));
            if (price.compareTo(user.getBalance()) <= 0) {
                // 得到打折后的余额
                // 修改用户的余额
                logger.debug("开始购买修改用户余额");
                user.setBalance((user.getBalance().subtract(price)));
                // 更新用户信息失败抛异常
                // 该方法更改了需要更改相应的参数
                state = userInformationService.modifyUser(String.valueOf(user.getId()), String.valueOf(user.getLevel()), String.valueOf(user.getBalance()), String.valueOf(user.getReport()));
                if (state.getState() == 1) {
                    logger.debug("购买成功");
                    // 添加订单信息,失败抛异常
                    getOrder(type,user.getId(),id,originalPrice,price);
//                    addOrder(user.getId(), id, type, singerId, albumId, classificationId, originalPrice, price);
                    logger.debug("订单添加成功");
                    // 用户购买音乐完成，开始修改用户收藏的音乐或MV的是否购买状态
//                    modifyCollectionAndSongList(user.getId(), id, type)
                }
            } else {
                logger.debug("余额不足");
                state.setInformation("余额不足，请立即充值");
            }
        } else {
            logger.debug("已购买过");
            state.setInformation("您已购买过，无需重复购买，如果无法播放请及时联系客服。");
        }
        return state;
    }

    /**
     * 用来得到指定活动的折扣
     */
    public float getDiscount(Integer activityId) {
        // 判断购买的商品是否参加了活动
        if (activityId != 0) {
            // 得到指定id的活动信息
            Activity activity = idExistence.isActivityId(activityId);
            // 判断活动是否存在
            if (activity != null) {
                // 得到活动的结束时间
                long endDate = activity.getEndDate().getTime();
                // 判断活动是否结束
                if (endDate >= System.currentTimeMillis()) {
                    // 得到指定活动的折扣
                    return activity.getDiscount();
                }
            }
        }
        return 1;
    }

    /**
     * 向数据库添加指定的订单信息
     */
    private void addOrder(int userId, int musicId, int type, int singerId, int albumId, int classificationId, BigDecimal originalPrice, BigDecimal price) throws DataBaseException {
        // 生成订单信息
        Order order = new Order();
        order.setUserId(userId);
        order.setMusicId(musicId);
        order.setType(type);
        order.setSingerId(singerId);
        order.setAlbumId(albumId);
        order.setClassificationId(classificationId);
        order.setPrice(originalPrice);
        order.setOriginalPrice(price);
        order.setMode("余额支付");
        order.setDate(new Date());
        // 向数据库添加订单信息
        if (orderMapper.insertOrder(order) < 1) {
            // 如果失败是数据库错误
            logger.error("订单：" + order + "添加订单信息时，数据库出错");
            throw new DataBaseException("订单：" + order + "添加订单信息时，数据库出错");
        }
    }

    /**
     * 用户购买订单完成更改用户收藏音乐或MV的购买状态，已经音乐的购买状态
     *
     * @param userId  用户的id
     * @param musicId 音乐或MV的id
     * @param type    1.表示音乐 2表示MV
     */
    public void modifyCollectionAndSongList(int userId, int musicId, int type) throws DataBaseException {
        // 判断用户是否收藏了该音乐或MV
        MusicCollect musicCollect = existenceService.isUserCollectionMusic(userId, musicId, type);
        if (musicCollect != null) {
            // 将状态修改为购买过了
            musicCollect.setHave(1);
            // 更改数据库的状态
            if (musicCollectMapper.updateMusicCollect(musicCollect) < 1) {
                // 如果失败是数据库错误
                logger.error("收藏：" + musicCollect + "更改收藏音乐或MV信息时，数据库出错");
                throw new DataBaseException("收藏：" + musicCollect + "更改收藏音乐或MV信息时，数据库出错");
            }
        }
        // 如果是音乐，找到该用户创建的所有歌单
        if (type == 1) {
            // 找到该用户创建的所有歌单
            List<SongList> list = aboutSongListService.userSongList(userId, 2);
            if (list != null) {
                for (SongList songList : list) {
                    MusicSongList musicSongList = new MusicSongList();
                    musicSongList.setBelongId(songList.getId());
                    musicSongList.setType(2);
                    // 查找指定歌单的所有音乐
                    List<MusicSongList> listMusicSong = musicSongListMapper.selectListMusicSongList(musicSongList);
                    // 遍历歌单中的所有音乐如果存在，指定音乐则进行信息更改
                    for (MusicSongList songListMusic : listMusicSong) {
                        // 判断用户的所有歌单中是否有该音乐，如果有则更改该音乐的状态
                        if (songListMusic.getMusicId() == musicId) {
                            songListMusic.setHave(1);
                            if (musicSongListMapper.updateMusicSongList(songListMusic) < 1) {
                                // 如果失败是数据库错误
                                logger.error("音乐列表：" + songListMusic + "更新音乐列表时，数据库出错");
                                throw new DataBaseException("音乐列表：" + songListMusic + "更新音乐列表时，数据库出错");
                            }
                            // 如果存在则直接结束当前循环
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * 充值VIP(事务)
     *
     * @param count 指定的充值几个月
     */
    @Transactional
    public State rechargeVIP(Integer count, HttpSession session) throws DataBaseException {
        logger.debug("充值的月数" + count);
        State state = new State();
        User user = specialFunctions.getUser(session);
        // 去数据库查找用户信息
        user = userMapper.selectUserMailbox(user.getMailbox());
        // 得到用户的余额
        BigDecimal balance = user.getBalance();
        logger.debug("用户余额" + balance);
        // 计算得到vip的价格
        BigDecimal price = BigDecimal.valueOf(count * 10);
        logger.debug("优惠前" + count);
        // 计算优惠前的价格
        int originalPrice =count;
        if (count == 6) {
            count++;
        } else if (count == 10) {
            count = count + 2;
        }
        logger.debug("优惠后" + count);
        logger.debug("vip的价格" + price);
        // 得到用户是否买的起
        if (balance.compareTo(price) > 0) {
            // 修改用户的余额
            user.setBalance(balance.subtract(price));
            // 如果用户本来就是vip则续费
            Calendar calendar = Calendar.getInstance();
            if (user.getVipDate().getTime() > System.currentTimeMillis()) {
                // 将原vip日期封装
                calendar.setTime(user.getVipDate());
                logger.debug("用户本来就是VIP，原vip时间" + user.getVipDate());
            } else {
                // 将日期封装
                calendar.setTime(new Date());
            }
            // 修改用户的账号等级
            if (user.getLevel() < 1) {
                user.setLevel(1);
            }
            // 向后推移几个月
            calendar.add(Calendar.MONTH, count);
            // 设置用户vip到期日期
            user.setVipDate(calendar.getTime());
            logger.debug("设置用户vip的时间" + user.getVipDate());
            // 更新用户信息失败抛异常
            // 该方法更改了需要更改相应的参数
            if (userMapper.updateUser(user) < 1) {
                // 如果失败是数据库错误
                logger.error("邮箱：" + user.getMailbox() + "修改用户信息时，数据库出错");
                throw new DataBaseException("邮箱：" + user.getMailbox() + "修改用户信息时，数据库出错");
            }
            // 4表示是充值VIp 然后用户ID，无音乐ID ，原价是VIP充值月数*10  现价是扣的钱
            getOrder(4,user.getId(),0,new BigDecimal(String.valueOf(count*10)),price);
            state.setState(1);
        } else {
            state.setInformation("余额不足，请立即充值");
        }
        return state;
    }

    /**
     * 判断用户是否购买了指定音乐或MV
     *
     * @param id   指定的音乐id
     * @param type 1表示音乐 2表示MV
     */
    public Order isPurchaseMusic(int id, int type, User user) {
        Order order = new Order();
        order.setMusicId(id);
        order.setUserId(user.getId());
        order.setType(type);
        List<Order> list = orderMapper.selectListOrder(order);
        if (list.size() > 0) {
            return list.get(0);
        }
        return null;
    }


    /**
     * 生成订单  1表示是音乐 2表示是MV 3表示充值 4表示VIP
     *
     * @param type          类型
     * @param userId        用户ID
     * @param musicId       如果是音乐MV音乐MV ID
     * @param originalPrice 原价
     * @param price         现价
     */
    public void getOrder(int type, int userId, int musicId, BigDecimal originalPrice, BigDecimal price) throws DataBaseException {
        Order order = new Order();
        switch (type) {
            case 1:
            case 2:
                order.setMusicId(musicId);
            case 4:
                order.setMode("余额");
                break;
            default:
                order.setMode("支付宝");
                break;

        }
        order.setType(type);
        order.setUserId(userId);
        order.setOriginalPrice(originalPrice);
        order.setPrice(price);
        order.setDate(new Date());
        if (orderMapper.insertOrder(order) < 1) {
            // 如果失败是数据库错误
            logger.error("添加时，数据库出错");
            throw new DataBaseException("添加时，数据库出错");
        }
    }
}
