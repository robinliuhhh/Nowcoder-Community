package com.nowcoder.community.controller;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectRequest;
import com.nowcoder.community.annotation.LoginRequired;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.FollowService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Controller
@RequestMapping("/user")
public class UserController implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

//    @Value("${community.path.upload}")
//    private String uploadPath;
//
//    @Value("${community.path.domain}")
//    private String domain;
//
//    @Value("${server.servlet.context-path}")
//    private String contextPath;

    @Value("${aliyun.endpoint}")
    private String endpoint;

    @Value("${aliyun.access-key-id}")
    private String accessKeyId;

    @Value("${aliyun.access-key-secret}")
    private String accessKeySecret;

    @Value("${aliyun.bucket-name}")
    private String bucketName;

    @Value("${aliyun.bucket-header-dir}")
    private String headerDir;

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private LikeService likeService;

    @Autowired
    private FollowService followService;

    @LoginRequired
    @GetMapping("/setting")
    public String getSettingPage() {
        return "/site/setting";
    }

    @LoginRequired
    @PostMapping("/upload-header")
    public String uploadHeaderToAliyun(MultipartFile headerImage, Model model) {
        if (headerImage == null) {
            model.addAttribute("error", "您还没有选择图片!");
            return "/site/setting";
        }

        String fileName = headerImage.getOriginalFilename();
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        if (StringUtils.isBlank(suffix)) {
            model.addAttribute("error", "文件的格式不正确!");
            return "/site/setting";
        }

        // 生成随机文件名
        fileName = headerDir + "/" + CommunityUtil.generateUUID() + suffix;
        // 创建OSSClient实例
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try {
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, fileName, headerImage.getInputStream());
            // 上传文件
            ossClient.putObject(putObjectRequest);
        } catch (IOException e) {
            logger.error("上传文件失败:" + e.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }

        // 更新当前用户的头像的路径（web访问路径）
        // https://robins-community.oss-cn-shanghai.aliyuncs.com/header/da394fbb73b147f99b4164f9f6d8f54c.png
        User user = hostHolder.getUser();
        String headerUrl = "https://" + bucketName + "." + endpoint + "/" + fileName;
        userService.updateHeader(user.getId(), headerUrl);

        return "redirect:/index";
    }

//    @Deprecated // 上传至阿里云OSS 此方法上传至本地 故废弃
//    @LoginRequired
//    @PostMapping("/upload")
//    public String uploadHeader(MultipartFile headerImage, Model model) {
//        if (headerImage == null) {
//            model.addAttribute("error", "您还没有选择图片!");
//            return "/site/setting";
//        }
//
//        String fileName = headerImage.getOriginalFilename();
//        String suffix = fileName.substring(fileName.lastIndexOf("."));
//        if (StringUtils.isBlank(suffix)) {
//            model.addAttribute("error", "文件的格式不正确!");
//            return "/site/setting";
//        }
//
//        // 生成随机文件名
//        fileName = CommunityUtil.generateUUID() + suffix;
//        // 确定文件存放的路径（dest目标路径）
//        File dest = new File(uploadPath + "/" + fileName);
//        try {
//            // 存储文件
//            headerImage.transferTo(dest);
//        } catch (IOException e) {
//            logger.error("上传文件失败: " + e.getMessage());
//            throw new RuntimeException("上传文件失败,服务器发生异常!", e);
//        }
//
//        // 更新当前用户的头像的路径（web访问路径）
//        // http://localhost:8080/community/user/header/xxx.png
//        User user = hostHolder.getUser();
//        String headerUrl = domain + contextPath + "/user/header/" + fileName;
//        userService.updateHeader(user.getId(), headerUrl);
//
//        return "redirect:/index";
//    }

//    // 不需要@LoginRequired 因为不登录也可以看别人的头像
//    // 图片是二进制流输出 通过HttpServletResponse直接输出 所以返回值是void
//    // 上传至本地时路径为 http://localhost:8080/community/user/header/xxx.png
//    // 此时刷新界面读到user.headerUrl时会调用此方法
//    @Deprecated
//    @GetMapping("/header/{fileName}")
//    public void getHeader(@PathVariable("fileName") String fileName, HttpServletResponse response) {
//        // 服务器存放路径
//        fileName = uploadPath + "/" + fileName;
//        // 文件后缀
//        String suffix = fileName.substring(fileName.lastIndexOf("."));
//        // 响应图片
//        response.setContentType("image/" + suffix);
//        try (
//                FileInputStream fis = new FileInputStream(fileName);
//                OutputStream os = response.getOutputStream();
//        ) {
//            // byte[1024] 一批一批输出 效率高
//            byte[] buffer = new byte[1024];
//            int b;
//            while ((b = fis.read(buffer)) != -1) {
//                os.write(buffer, 0, b);
//            }
//        } catch (IOException e) {
//            logger.error("读取头像失败: " + e.getMessage());
//        }
//    }

    // 个人主页
    @GetMapping("/profile/{userId}")
    public String getProfilePage(@PathVariable("userId") int userId, Model model) {
        User user = userService.getById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在!");
        }

        // 用户
        model.addAttribute("user", user);
        // 点赞数量
        int likeCount = likeService.findUserLikeCount(userId);
        model.addAttribute("likeCount", likeCount);
        // 关注数量
        long followeeCount = followService.findFolloweeCount(userId, ENTITY_TYPE_USER);
        model.addAttribute("followeeCount", followeeCount);
        // 粉丝数量
        long followerCount = followService.findFollowerCount(ENTITY_TYPE_USER, userId);
        model.addAttribute("followerCount", followerCount);
        // 是否已关注
        boolean hasFollowed = false;
        if (hostHolder.getUser() != null) {
            hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
        }
        model.addAttribute("hasFollowed", hasFollowed);

        return "/site/profile";
    }

}
