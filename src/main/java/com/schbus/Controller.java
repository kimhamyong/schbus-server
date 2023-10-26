package com.schbus;

import org.springframework.ui.Model;
import com.schbus.pay.KakaoPay;
import jakarta.servlet.http.HttpSession;
import lombok.Setter;
import lombok.extern.java.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Log
@org.springframework.stereotype.Controller
public class Controller {
    private static final Logger logger = LoggerFactory.getLogger(Controller.class);
    @Autowired
    private PersonRepository personRepository;
    @Autowired
    private ReserveRepository reserveRepository;

    private ReserveDto tmp= new ReserveDto();
    @Setter(onMethod_ = @Autowired)
    private KakaoPay kakaopay;

    @PostMapping("/select_bus")
    public ResponseEntity<String> selectBus(@RequestBody ReserveDto reserveDto) {
        String selectedBus = reserveDto.getBus();
        String userId = reserveDto.getUser();

        logger.info(selectedBus);
        logger.info(userId);
        return ResponseEntity.ok("버스가 선택되었습니다.");
    }


    @PostMapping("/select_date") // 안드에서 날짜랑 버스값 받아와야 됨
    public ResponseEntity<?> selectDate(@RequestBody ReserveDto reserveDto) {
        String selectedBus = reserveDto.getBus();
        String selectedDate = reserveDto.getDate();

        // logger.info(selectBus);
        // logger.info(selectedDate);

        List<Reserve> reserves = reserveRepository.findByBusAndDate(selectedBus, selectedDate); //선택된 버스와 날짜에 대한 모든 예약 정보를 조회
        List<String> reservedSeats = reserves.stream()
               .map(reserve -> reserve.getSeat().toString())
               .collect(Collectors.toList()); //최종적으로 resrvedSeats 리스트에 예약된 좌석 목록을 저장
        // logger.info(reservedSeats.toString());

        return ResponseEntity.ok(reservedSeats);
    } //여기서 넘어가서 좌석 창이 보이게 할 때, 안드로이드에서 예약 가능 좌석 불가능 좌석 ui로 구현

    @PostMapping("/select_seat") //좌석 선택 -> 카카오페이 결제
    public ResponseEntity<?> selectSeat(@RequestBody ReserveDto reserveDto, HttpSession session) {
        tmp.setBus(reserveDto.getBus());
        tmp.setDate(reserveDto.getDate());
        tmp.setSeat(reserveDto.getSeat());
        tmp.setUser(reserveDto.getUser());


        return ResponseEntity.ok(kakaopay.kakaoPayReady().getNext_redirect_mobile_url()); //카카오페이 결제창으로 이동하기 위한 url
    }

    @GetMapping ("/kakaoPaySuccess") //결제 성공 후 db에 저장
    public ResponseEntity<String> kakaoPaySuccess(@RequestParam("pg_token") String pg_token, Model model) {
        String bus = tmp.getBus();
        String date = tmp.getDate();
        String seat = tmp.getSeat();
        String user = tmp.getUser();

        Reserve list = new Reserve(); // 추가한 정보를 Reserve 객체에 set

        list.setBus(bus);
        list.setDate(date);
        list.setSeat(seat);
        list.setUser(user);

        reserveRepository.save(list); //db에 저장

        return ResponseEntity.ok("좌석이 예약되었습니다.");
    }

    @PostMapping("/check_my_ticket") //예약 확인
    public ResponseEntity<?> checkMyTicket(@RequestBody ReserveDto recvPurchase) {
        Optional<Reserve> reserveOptional = reserveRepository.findByUser(recvPurchase.getUser());
        if (reserveOptional.isPresent()) {
            Reserve reserve = reserveOptional.get();
            ReserveDto reservationInfo = new ReserveDto();
            // reservationInfo.setUser(reserve.getUser().toString());
            // logger.info(reserve.getUser().toString());
            reservationInfo.setBus(reserve.getBus().toString());
            logger.info(reserve.getBus().toString());
            reservationInfo.setDate(reserve.getDate().toString());
            logger.info(reserve.getDate().toString());
            reservationInfo.setSeat(reserve.getSeat().toString());
            logger.info(reserve.getSeat().toString());
            return ResponseEntity.ok(reservationInfo);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("내역이 없습니다.");
    }

    @PostMapping("/login_app")
    public ResponseEntity<String> login_app(@RequestBody PersonDto recvPerson, HttpSession session) {
    	String userId = recvPerson.getUser_id();
        String password = recvPerson.getPassword();

        // SignupDto를 Person 객체로 변환
        Person person = new Person();
        person.setUserId(userId);
        person.setUserPwd(password);

        logger.info(userId);

        Optional<Person> foundPerson = personRepository.findByUserId(userId);

        if (foundPerson.isPresent() && foundPerson.get().getUserPwd().equals(password)) {
            session.setAttribute("userId", userId); // 로그인한 사용자의 ID를 세션에 저장합니다.
            return ResponseEntity.ok("Success");
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("아이디와 비밀번호를 확인하세요.");
    }
    
    @PostMapping("/signup_app")
    public ResponseEntity<String> signup_app(@RequestBody PersonDto recvPerson) {
        String userId = recvPerson.getUser_id();
        String password = recvPerson.getPassword();
        String name = recvPerson.getName();

        // SignupDto를 Person 객체로 변환
        Person person = new Person();
        person.setUserId(userId);
        person.setUserPwd(password);
        person.setUserName(name);

        if (personRepository.findByUserId(recvPerson.getUser_id()).isPresent()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("사용중인 아이디 입니다.");

        }
        personRepository.save(person);
        return ResponseEntity.ok("회원 가입에 성공하였습니다.");
    }

}
