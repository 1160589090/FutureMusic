package controller.music.search;


import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import entity.Classification;
import entity.SingerExt;
import entity.User;
import mapper.UserMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import service.music.ActivityService;
import service.music.MusicService;
import service.music.MusicVideoService;
import service.music.SingerService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * @author 蒋靓峣 5.30 修改
 */
@Controller
public class SearchSinger {

    @Resource(name = "SingerService")
    SingerService singerService;
    @Resource(name ="UserMapper")
    UserMapper userMapper;
    /**
     * 搜索歌手的详细信息
     */
//    public Map<String,Object> searchSin
    /**
     * 点击搜索歌手，ajax
      *@param request 接收请求
     * @return PageInfo 返回匹配到的专辑、歌曲、歌手、MV信息
     */
    @RequestMapping(value = "/searchListSingerByName")
    @ResponseBody
    public PageInfo searchListSingerByName(HttpServletRequest request) {
        String pn = request.getParameter("pn");
        PageHelper.startPage(Integer.parseInt(pn), 10);
        String keyWord = request.getParameter("keyWord");
        List<SingerExt> singerList = singerService.exhibitionSingersByName(keyWord);
        PageInfo page = new PageInfo(singerList, 5);
        return page;
    }
    /**
     * 搜索音乐人
     */
    @RequestMapping(value = "/exhibitionSingersByRegion")
    @ResponseBody
    public List<SingerExt> exhibitionSingersByRegion(HttpServletRequest request){
        String  singerRegion = request.getParameter("singerRegion");
        return singerService.exhibitionSingersByRegion(singerRegion);
    }
    /**
     * 搜索全部
     */
    @RequestMapping(value = "/allSinger")
    @ResponseBody
    public List<SingerExt> allSinger(HttpServletRequest request){
        User user = new User();
        user.setLevel(2);
        return singerService.searchAllSinger();
    }

}
