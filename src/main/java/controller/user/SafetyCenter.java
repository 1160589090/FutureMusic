package controller.user;

import entity.State;
import org.springframework.ui.Model;
import service.user.SpecialFunctions;
import service.user.SecretProtectionService;
import util.exception.DataBaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import service.user.SafetyCenterService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * 安全中心页面的功能
 * 修改密码，找回密码，设置密保，修改密保（邮箱验证修改）
 *
 * @author 5月18日 张易兴创建
 */
@Controller
public class SafetyCenter {
    @Resource(name = "SecretProtectionService")
    SecretProtectionService secretProtectionService;
    @Resource(name = "SafetyCenterService")
    SafetyCenterService safetyCenterService;
    @Resource(name = "SpecialFunctions")
    SpecialFunctions specialFunctions;
    private static final Logger logger = LoggerFactory.getLogger(SafetyCenter.class);

    /**
     * 跳转带安全中心的页面
     * 如果会话上绑定过用户邮箱则跳转到安全中心首页，否则跳转到绑定邮箱页面
     */
    @RequestMapping(value = "/safetyCenterPage")
    public String safetyCenterPage(HttpSession session, Model model) {
        logger.trace("safetyCenterPage方法开始执行");
        return safetyCenterService.safetyCenterPage(session,model);
    }


    /**
     * 绑定邮箱，如果成功进行页面跳转否则还是本页，ajax
     * @param mailbox 用户需要绑定的邮箱
     * @return State 状态码为1时跳转到安全中心首页
     */
    @RequestMapping(value = "/bindingMailbox")
    @ResponseBody
    public State bindingMailbox(String mailbox, HttpSession session) {
        logger.trace("bindingMailbox方法开始执行");
        return safetyCenterService.bindingMailbox(mailbox,session);
    }

    /**
     * 点击忘记密码跳转到邮箱验证页面
     */
    @RequestMapping(value = "/mailboxBindingMailboxVerification")
    public String mailboxVerification( HttpSession session,Model model) {
        return safetyCenterService.mailboxVerification(session,model);
    }


    /**
     * 取消邮箱绑定，ajax
     */
    @RequestMapping(value = "/cancelBinding")
    public String cancelBinding(HttpServletRequest request, HttpServletResponse response,HttpSession session,Model model) {
        logger.trace("bindingMailbox方法开始执行");
        return safetyCenterService.cancelBinding(request,response,session,model);
    }


    /**
     * 用于验证原密码是否正确
     */
    @RequestMapping(value = "/mailboxBindingVerifyOriginalPassword")
    @ResponseBody
    public State  verifyOriginalPassword(String originalPassword, HttpSession session) {
        return safetyCenterService.verifyOriginalPassword( originalPassword,  session);
    }

    /**
     * 用于密码显示修改密码页面
     */
    @RequestMapping(value = "/mailboxBindingPasswordChangePasswordPage")
    public String  passwordChangePasswordPage(HttpSession session,Model model) {
        return safetyCenterService.passwordChangePasswordPage(session,model);
    }




    /**
     * 给绑定的邮箱，发送验证码
     * 用于找回密码
     *
     * @param session 获取当前会话的对象
     */
    @RequestMapping(value = "/mailboxBindingSecretProtectionVerificationCode")
    @ResponseBody
    public State secretProtectionVerificationCode(HttpSession session) {
        logger.trace("secretProtectionVerificationCode方法开始执行");
        return specialFunctions.sendVerificationCode(specialFunctions.getUserMailbox(session),session);
    }


    /**
     * 邮箱验证提交验证码
     */
    @RequestMapping(value = "/mailboxBindingMailboxVerificationCode")
    @ResponseBody
    public State mailboxVerificationCode(String verificationCode, HttpSession session) {
        return safetyCenterService.mailboxVerificationCode(verificationCode,session);
    }


    /**
     * 用于邮箱显示修改密码页面
     */
    @RequestMapping(value = "/mailboxBindingMailboxChangePasswordPage")
    public String  mailboxChangePasswordPage(HttpSession session,Model model) {
        return safetyCenterService.mailboxChangePasswordPage(session,model);
    }

    /**
     * 点击修改密码执行此方法，ajax
     *
     * @param password         接收输入的新密码
     * @param passwordAgain    接收再次输入的新密码
     * @param session          获取当前会话的对象
     */
    @RequestMapping(value = "/mailboxBindingChangePassword")
    @ResponseBody
    public State changePassword( String password, String passwordAgain, HttpSession session, HttpServletRequest request, HttpServletResponse response) throws DataBaseException {
        logger.trace("changePassword方法开始执行");
        return safetyCenterService.changePassword(password, passwordAgain, session, request, response);
    }











    /**
     * 点击设置密保或修改密保执行此方法，ajax
     * 修改密保的邮箱修改
     *
     * @param verificationCode 用户输入的验证码
     * @param gender           密保——爱好
     * @param age              密保——特长
     * @param birthday         密保——梦想
     * @param address          密保——愿望
     * @param session          当前会话
     */
    @RequestMapping(value = "/addSecretProtection")
    @ResponseBody
    public State addSecretProtection(String verificationCode, String gender, String age, String birthday, String address, HttpSession session) throws DataBaseException {
        logger.trace("addSecretProtection方法开始执行");
        return secretProtectionService.addSecretProtection(verificationCode, gender, age, birthday, address, session);
    }

    /**
     * 找回密码（通过密保找回，忘记密保通过邮箱验证找回）
     * 第一步，先验证账号是否有密保
     *
     */
    @RequestMapping(value = "/verificationAccount")
    @ResponseBody
    public State verificationAccount(HttpSession session) {
        logger.trace("verificationAccount方法开始执行");
        return secretProtectionService.verificationAccount(specialFunctions.getUserMailbox(session));
    }

    /**
     * 找回密码
     * 第二步，验证账号的密保是否正确
     * 第三步，调用设置密保
     *
     * @param gender   密保——性别
     * @param age      密保——年龄
     * @param birthday 密保——出生日期
     * @param address  密保——住址
     */
    @RequestMapping(value = "/verificationSecretProtection")
    @ResponseBody
    public State verificationSecretProtection( String gender, String age, String birthday, String address,HttpSession session) {
        logger.trace("verificationSecretProtection方法开始执行");
        return secretProtectionService.verificationSecretProtection( gender, age, birthday, address,session);
    }

}
