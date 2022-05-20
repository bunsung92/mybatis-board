package com.nhnacademy.jdbc.board.post.web.controller;

import com.nhnacademy.jdbc.board.exception.ModifyAccessException;
import com.nhnacademy.jdbc.board.exception.UserNotFoundException;
import com.nhnacademy.jdbc.board.exception.ValidationFailedException;
import com.nhnacademy.jdbc.board.post.dto.request.PostInsertRequest;
import com.nhnacademy.jdbc.board.post.dto.request.PostModifyRequest;
import com.nhnacademy.jdbc.board.post.dto.response.PostResponse;
import com.nhnacademy.jdbc.board.post.service.PostService;
import com.nhnacademy.jdbc.board.user.dto.response.UserLoginResponse;
import jakarta.validation.Valid;
import java.util.Objects;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;


@Slf4j
@Controller
@RequestMapping("/post")
@RequiredArgsConstructor
@Validated
public class PostController {

    private final PostService postService;

    @GetMapping(value = "/write")
    public ModelAndView insert(PostInsertRequest postInsertRequest) {
        return new ModelAndView("post/post-form").addObject("post", postInsertRequest);

    }

    @PostMapping(value = "/write")
    public ModelAndView doInsert(@ModelAttribute @Valid PostInsertRequest postInsertRequest, BindingResult bindingResult, HttpServletRequest request) {

        HttpSession session = request.getSession(true);
        UserLoginResponse userLoginResponse = (UserLoginResponse) session.getAttribute("user");

        if (Objects.isNull(userLoginResponse)) {
            throw new UserNotFoundException();
        }

        if (bindingResult.hasErrors()){
            throw new ValidationFailedException(bindingResult);
        }

        postInsertRequest.setUserNo(userLoginResponse.getUserNo());

        ModelAndView mav = new ModelAndView("redirect:posts");
        mav.addObject("url", "write");
        postService.insertPost(postInsertRequest);

        return mav;
    }

    @GetMapping("/{postNo}")
    public ModelAndView post(@PathVariable("postNo") Long postNo) {

        ModelAndView mav = new ModelAndView("post/post");

        mav.addObject("post", postService.findPostByNo(postNo));
        return mav;
    }

    @GetMapping("/posts")
    public ModelAndView posts(@RequestParam(name = "page", required = false) Integer page) {

        page = Optional.ofNullable(page).orElse(1);

        if (page < 1) {
            return new ModelAndView("redirect:posts?page=1");
        }

        int totalPage = postService.getTotalPage();
        if (page > totalPage) {
            return new ModelAndView("redirect:posts?page=" + totalPage);
        }

        ModelAndView mav = new ModelAndView("post/posts");
        mav.addObject("page", postService.findPagedPosts(page, totalPage));
        return mav;
    }

    @GetMapping(value = "/modify/{postNo}")
    public ModelAndView modify(@PathVariable(name = "postNo") Long postNo, HttpSession session) {

        UserLoginResponse user = (UserLoginResponse) session.getAttribute("user");

        if (canNotModify(postNo, user)) {
            throw new ModifyAccessException();
        }

        PostResponse post = postService.findPostByNo(postNo);

        ModelAndView mav = new ModelAndView("post/post-form");

        mav.addObject("url", "/post/modify/" + postNo);
        mav.addObject("post", post);
        mav.addObject("title", post.getTitle());
        mav.addObject("content", post.getContent());
        mav.addObject("createAt", post.getCreatedAt());

        return mav;
    }

    @PostMapping(value = "/modify/{postNo}")
    public ModelAndView doModify(@ModelAttribute PostModifyRequest request, HttpSession session) {

        UserLoginResponse user = (UserLoginResponse) session.getAttribute("user");

        if (canNotModify(request.getPostNo(), user)) {
            throw new ModifyAccessException();
        }

        request.setModifyUserNo(user.getUserNo());
        postService.modifyPost(request);

        return new ModelAndView("redirect:/post/posts");
    }

    private boolean canNotModify(Long postNo, UserLoginResponse user) {
        return !(user.isAdmin() || postService.isWriter(postNo, user.getUserNo()));
    }
}
