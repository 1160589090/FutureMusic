package controller.user.administrators;

import com.github.pagehelper.PageInfo;
import entity.SongList;
import entity.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import service.user.administrators.ActivityInformationService;
import service.user.administrators.SongListInformationService;
import util.SameUrlData;
import util.exception.DataBaseException;

import javax.annotation.Resource;

/**
 * 专辑
 * 显示id，用户id，分类，创建的时间，活动id
 * 更多：标题，图片，介绍
 * 查询：1、id
 * 2、活动id
 * 3、分类的id
 * 4、用户的id
 * 修改：专辑用于修改活动
 * 删除：id
 * * 查找指定专辑或歌单中的所有音乐
 * 查询：指定专辑或歌单被收藏的次数
 *
 * @author 5月22日 张易兴创建
 */
@Controller
@RequestMapping(value = "/administrators")
public class SongListInformation {
    private static final Logger logger = LoggerFactory.getLogger(ActivityInformationService.class);
    @Resource(name = "SongListInformationService")
    SongListInformationService songListInformationService;

    /**
     * 显示歌单或专辑的信息
     *
     * @param condition 1表示歌单或专辑的id，2表示活动的id 3、分类的id  4、歌手的id  5、type
     */
    @RequestMapping(value = "/showSongList")
    public String showSongList(String[] condition, @RequestParam(defaultValue="1")Integer pageNum, Model model) {
        return songListInformationService.showSongList(condition, pageNum,model);
    }
    /**
     * 得到指定id的歌单或专辑信息+收藏量
     */
    @RequestMapping(value = "/showIdSongList")
    @ResponseBody
    public SongList showIdSongList(Integer id){
        return songListInformationService.showIdSongList(id);
    }


    /**
     * 修改专辑的活动，ajax
     */
    @RequestMapping(value = "/modifySongList")
    @ResponseBody
    public State modifySongList(String id,String activity ) throws DataBaseException {
        return songListInformationService.modifySongList(id,activity);
    }

    /**
     * 删除指定id专辑或歌单
     */
    @RequestMapping(value = "/deleteSongList")
    @ResponseBody
    public State  deleteSongList(String id) throws DataBaseException {
        return songListInformationService.deleteSongList(id);
    }
}
