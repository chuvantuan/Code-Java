/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.ticket_manager.service;

import com.mycompany.ticket_manager.model.Response;
import com.mycompany.ticket_manager.model.Seat;
import com.mycompany.ticket_manager.model.Ticket;
import com.mycompany.ticket_manager.repository.CalendarRepository;
import com.mycompany.ticket_manager.repository.MovieRepository;
import com.mycompany.ticket_manager.repository.SeatRepository;
import com.mycompany.ticket_manager.repository.StaffRepository;
import com.mycompany.ticket_manager.repository.TicketRepository;
import com.mycompany.ticket_manager.util.NumberUtil;
import com.mycompany.ticket_manager.util.Timestamp;
import com.mycompany.ticket_manager.util.valid.EmailValid;
import io.github.cdimascio.dotenv.Dotenv;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.thymeleaf.context.Context;

/**
 *
 * @author Viet
 */
public class TicketService {

    Dotenv dotenv = Dotenv.load();
    MovieRepository movieRepository;
    CalendarRepository calendarRepository;
    StaffRepository staffRepository;
    TicketRepository ticketRepository;
    SeatRepository seatRepository;
    MailService mailService;
    RenderService renderService;

    public TicketService() {
        this.movieRepository = new MovieRepository();
        this.calendarRepository = new CalendarRepository();
        this.staffRepository = new StaffRepository();
        this.ticketRepository = new TicketRepository();
        this.seatRepository = new SeatRepository();
        this.mailService = new MailService();
        this.renderService = new RenderService();
    }

    public Response<Map<String, Object>> getPriceService(int quantity, int object) {
        try {
            if (quantity < 0 || quantity > 100) {
                return new Response<>("Sai số lượng");
            }
            String name;
            if (object == 1) {
                name = "PRICE_POPCORN";
            } else if (object == 2) {
                name = "PRICE_WATER";
            } else {
                throw new Exception("Đối tượng không xác định");
            }
            String priceString = dotenv.get(name);
            if (priceString == null || priceString.trim().equals("")) {
                System.out.println("Không có nội dung " + name);
                return new Response<>("Lỗi xử lý");
            }
            int price = 0;
            try {
                price = Integer.parseInt(priceString);
                if (price <= 0) {
                    throw new Exception("Giá không hợp lệ");
                }
            } catch (Exception e) {
                System.out.println("Lỗi chuyển chuỗi");
                e.printStackTrace();
                return new Response<>("Lỗi xử lý");
            }
            int total = price * quantity;
            Map<String, Object> data = new HashMap<String, Object>() {
                {
                    put("num", total);
                    put("str", NumberUtil.convertPrice(total));
                }
            };
            return new Response<Map<String, Object>>().ok(data);
        } catch (Exception e) {
            e.printStackTrace();
            return new Response<>("Có lỗi xảy ra");
        }
    }

    public Response<Map<String, Object>> getPriceChairs(List<String> listChair, String movie) {
        try {

            ResultSet resultSet = this.movieRepository.findById(movie);
            if (resultSet == null) {
                return new Response<>("Lỗi truy vấn dữ liệu");
            }

            resultSet.next();

            if (resultSet.getRow() == 0) {
                return new Response<>("Không có dữ liệu");
            }

            int price = resultSet.getInt("minPrice");
            int total = price * listChair.size();
            Map<String, Object> data = new HashMap<String, Object>() {
                {
                    put("num", total);
                    put("str", NumberUtil.convertPrice(total));
                }
            };
            return new Response<Map<String, Object>>().ok(data);
        } catch (Exception e) {
            e.printStackTrace();
            return new Response<>("Có lỗi xảy ra");
        }
    }

    public Response<Ticket> checkInfo(Ticket ticket, List<String> chairs) {
        try {
            ticket.setName(ticket.getName().trim());
            ticket.setEmail(ticket.getEmail().trim());
            if (ticket.getName().equals("")) {
                return new Response<>("Trống tên khách hàng");
            }
            if (ticket.getEmail().equals("")) {
                return new Response<>("Trống email");
            }

            if (!EmailValid.isEmail(ticket.getEmail())) {
                return new Response<>("Email không đúng");
            }

            if (chairs.isEmpty()) {
                return new Response<>("Không có ghế nào được chọn");
            }

            ResultSet resultSet = this.calendarRepository.getCalendar(ticket.getCalendar());
            if (resultSet == null) {
                return new Response<>("Lỗi truy vấn dữ liệu");
            }

            resultSet.next();

            if (resultSet.getRow() == 0) {
                return new Response<>("Lịch chiếu không tồn tại");
            }
            resultSet = this.staffRepository.getStaffById(ticket.getCreateBy());
            if (resultSet == null) {
                return new Response<>("Lỗi truy vấn dữ liệu");
            }

            resultSet.next();

            if (resultSet.getRow() == 0) {
                return new Response<>("Nhân viên không xác định");
            }

            String id = "TICKET_" + Timestamp.getNowTimeStamp() + "_" + NumberUtil.genNumber(3);
            ticket.setId(id);
            return new Response<Ticket>().ok(ticket);
        } catch (Exception e) {
            e.printStackTrace();
            return new Response<>("Có lỗi xảy ra");
        }
    }

    public Response<Boolean> addTicket(Ticket ticket, List<String> chairs, String nameMovie, String playAt) {
        try {
            ticket.setCreateAt(Timestamp.getNowTimeStamp());
            int insertTicket = this.ticketRepository.insertTicket(ticket);
            if (insertTicket == -1) {
                return new Response<>("Lỗi try vấn dữ liệu");
            }
            if (insertTicket == 0) {
                return new Response<>("Tạo vé thất bại");
            }

            List<Seat> dataSeat = new ArrayList<>();

            for (String location : chairs) {
                dataSeat.add(new Seat(ticket.getId(), location, ticket.getCalendar()));
            }

            int inserted = this.seatRepository.insertListSeat(dataSeat);

            if (inserted == -1) {
                return new Response<>("Lỗi truy vấn dữ liệu");
            }

            Context context = new Context();
            context.setVariable("movie", nameMovie);
            context.setVariable("calendar", playAt);
            context.setVariable("chairs", String.join(", ", chairs));
            context.setVariable("ticket", ticket.getId());
            context.setVariable("customer", ticket.getName());

            String body = this.renderService.run(context, "mail_ticket");

            this.mailService.senMail("Thông tin vé phim", body, ticket.getEmail());

            return new Response<Boolean>().ok(true);
        } catch (Exception e) {
            e.printStackTrace();
            return new Response<>("Có lỗi xảy ra");
        }
    }

    public Response<Map<String, String>> getInfoTicket(String id) {
        try {
            if (id == "") {
                return new Response<>("Không tìm thấy vé");
            }

            ResultSet resultSet = this.ticketRepository.getInfoTicket(id);
            resultSet.next();
            if (resultSet.getRow() == 0) {
                return new Response<>("Không tìm thấy vé");
            }

            if (resultSet.getString("checkinAt") != null) {
                return new Response<>("Vé đã sử dụng");
            }
            long now = Timestamp.getNowTimeStamp();
            if (now > resultSet.getLong("time") + resultSet.getLong("timeMovie") * 60) {
                return new Response<>("Phim đã dừng chiếu");
            }

            if (now < resultSet.getLong("time") - 10 * 60) {
                return new Response<>("Xem quá sớm");
            }

            Map<String, String> data = new HashMap<>() {
                {
                    put("id", resultSet.getString("id"));
                    put("time", Timestamp.convertTimeStampToString(resultSet.getLong("time"), "HH:mm dd-MM-yyyy"));
                    put("room", resultSet.getString("room"));
                    put("room", resultSet.getString("room"));
                    put("person", resultSet.getString("numPerson"));
                    put("popcorn", resultSet.getString("numPopcorn"));
                    put("water", resultSet.getString("numWater"));

                }
            };

            ResultSet resultSet1 = this.seatRepository.getSeat(id);
            List<String> listChair = new ArrayList<>();
            while (resultSet1.next()) {
                listChair.add(resultSet1.getString("location"));
            }

            data.put("location", String.join(", ", listChair));

            return new Response<Map<String, String>>().ok(data);

        } catch (Exception e) {
            e.printStackTrace();
            return new Response<>("Có lỗi xảy ra");
        }
    }

    public Response<Boolean> checkIn(String id, String staff) {
        try {
            long now = Timestamp.getNowTimeStamp();
            int update = this.ticketRepository.checkIn(id, now, staff);
            if (update <= 0) {
                return new Response<>("Cập nhật thất bại");
            }

            return new Response<Boolean>().ok(true);

        } catch (Exception e) {
            e.printStackTrace();
            return new Response<>("Có lỗi xảy ra");
        }
    }

}
