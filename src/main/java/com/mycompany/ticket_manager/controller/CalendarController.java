/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.ticket_manager.controller;

import com.mycompany.ticket_manager.model.Response;
import com.mycompany.ticket_manager.service.CalendarService;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Viet
 */
public class CalendarController {

    CalendarService calendarService;

    public CalendarController() {
        this.calendarService = new CalendarService();
    }

    public Response<List<Map<String, String>>> getCalendarMovie(String idMovie, long timestamp) {
        return this.calendarService.getCalendarMovie(idMovie, timestamp);
    }

    public Response<String> getRemaingTime(String idCalendar) {
        return this.calendarService.getRemaingTime(idCalendar);
    }

    public Response<List<Map<String, Object>>> getCalendar(long date, String selectedRoom) {
        return this.calendarService.getCalendar(date, selectedRoom);
    }

    public Response<Map<String, Object>> addCalendar(String playAt, String room, String movie) {
        return this.calendarService.addCalendar(playAt, room, movie);
    }

    public Response<Boolean> cancleCalendar(String id) {
        return this.calendarService.cancleCalendar(id);
    }
    
    public Response<Map<String, Object>> updatedCalendar(String id, String playAt, String room, String movie) {
        return this.calendarService.updateCalendar(id, playAt, room, movie);
    }

}
