package com.developer.user.command.service;

import com.developer.user.command.dto.*;
import com.developer.user.command.entity.User;
import com.developer.user.command.repository.UserRepository;
import com.developer.common.exception.CustomException;
import com.developer.common.exception.ErrorCode;
import com.developer.user.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserCommandService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;


    // 회원가입
    @Transactional
    public void registerUser(RegisterUserDTO userDTO) throws ParseException {

        if (checkUserId(userDTO.getUserId())){

            // 비밀번호 암호화
            String encode = passwordEncoder.encode(userDTO.getUserPw());

            // userDTO에 비밀번호 넣기
            userDTO.setUserPw(encode);
            Date userDate = convertStringToDate(userDTO.getUserBirth());
            User user = new User(userDTO, userDate);

            // User 객체로 변환 및 생성
//            User map = modelMapper.map(userDTO, User.class);

            log.info("User 객체 생성 {}", user);

            // DB에 저장
            userRepository.save(user);
        }
    }

    // 로그인
    @Transactional
    public SessionSaveDTO loginUser(LoginUserDTO userDTO){

        // 1. Login ID/PW 를 기반으로 AuthenticationToken 생성
        UsernamePasswordAuthenticationToken authenticationToken = userDTO.toAuthentication();

        // 2. 실제로 검증 (사용자 비밀번호 체크) 이 이루어지는 부분
        //    authenticate 메서드가 실행이 될 때 CustomUserDetailsService 에서 만들었던 loadUserByUsername 메서드가 실행됨
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

        // 3. 사용자 역할 정보를 가져옴
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        // 4. SessionSaveDTO에 값 넣기
        // CustomUserDetailsService에서 리턴한 User 객체를 사용하여 userCode와 userId를 가져옴
        Long userCode = userDetails.getUserCode();
        String userId = userDetails.getUsername(); // userId는 username으로 사용
        List<GrantedAuthority> authorities = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)    // 권한을 String으로 변환
                .map(SimpleGrantedAuthority::new) // String을 SimpleGrantedAuthority로 변환
                .collect(Collectors.toList());

        log.info("userCode {}, userId {}", userCode, userId);

        // 5. DTO 반환
        return new SessionSaveDTO(userCode, userId, authorities);

        // Security 도입 이전의 기존 코드
//        if(!passwordEncoder.matches(userDTO.getUserPw(), byUserID.getUserPw())){
//            throw new CustomException(ErrorCode.NOT_MATCH_PASSWORD);
//        }
//
//        return new SessionSaveDTO(byUserID.getUserCode(), byUserID.getUserId());
    }

    // 회원 정보 수정
    @Transactional
    public void updateUser(String userId, UpdateUserDTO updateUserDTO) throws ParseException {
        User byUserID = findByUserID(userId);

        updateUserDTO.setUserPw(passwordEncoder.encode(updateUserDTO.getUserPw()));

        byUserID.updateUser(updateUserDTO);

        userRepository.save(byUserID);
    }
    
//    // 회원 정보 조회
//    @Transactional
//    public ResponseUserDTO userDetail(String userId){
//        User byUserID = findByUserID(userId);
//
//        ResponseUserDTO map = modelMapper.map(byUserID, ResponseUserDTO.class);
//        log.info("ResponseUserDTO {}", map);
//
//        return map;
//    }

    // 회원탈퇴 (상태 변경)
    @Transactional
    public void deleteUser(String userId){
        User byUserID = findByUserID(userId);

        byUserID.deleteUser();

        log.info("탈퇴 성공 userStatus {}", byUserID.getUserStatus());

        userRepository.save(byUserID);
    }

    // 사용자 아이디로 User 객체 찾기
    @Transactional
    public User findByUserID(String userId){
        Optional<User> byUserId = userRepository.findByUserId(userId);

        if (byUserId.isEmpty()){
            log.info("아이디가 존재하지 않음 {}", userId);
            throw new CustomException(ErrorCode.NOT_FOUND_USER);
        }

        return byUserId.get();
    }

    // 아이디 중복 검증
    @Transactional
    public boolean checkUserId(String userId){
        Optional<User> byUserId = userRepository.findByUserId(userId);

        if (byUserId.isPresent()){
            log.info("아이디 값 중복 {}", userId);
            throw new CustomException(ErrorCode.DUPLICATE_USERID);
        }

        return true;
    }

    // 이메일 중복 검증
    @Transactional
    public boolean checkUserEmail(String userEmail){
        Optional<User> byUserEmail = userRepository.findByUserEmail(userEmail);

        if (byUserEmail.isPresent()){
            log.info("이메일 값 중복 {}", userEmail);
            throw new CustomException(ErrorCode.DUPLICATE_USEREMAIL);
        }

        return true;
    }

    // 닉네임 중복 검증
    @Transactional
    public boolean checkUserNick(String userNick){
        Optional<User> byUserNick = userRepository.findByUserNick(userNick);

        if (byUserNick.isPresent()){
            log.info("닉네임 값 중복 {}", userNick);
            throw new CustomException(ErrorCode.DUPLICATE_USERNICK);
        }

        return true;
    }

    // 핸드폰 번호 중복 검증
    @Transactional
    public boolean checkUserPhone(String userPhone){
        Optional<User> byUserPhone = userRepository.findByUserPhone(userPhone);

        if (byUserPhone.isPresent()){
            log.info("핸드폰 번호 값 중복 {}", userPhone);
            throw new CustomException(ErrorCode.DUPLICATE_USERPHONE);
        }

        return true;
    }

    // 날짜 변환 메서드
    public Date convertStringToDate(String dateString) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return dateFormat.parse(dateString);
    }
}