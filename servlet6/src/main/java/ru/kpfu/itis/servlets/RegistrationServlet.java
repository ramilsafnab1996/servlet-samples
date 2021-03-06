package ru.kpfu.itis.servlets;

import ru.kpfu.itis.entities.User;
import ru.kpfu.itis.exceptions.*;
import ru.kpfu.itis.exceptions.SecurityException;
import ru.kpfu.itis.repositories.UserRepository;
import ru.kpfu.itis.utilities.SecurityService;
import ru.kpfu.itis.utilities.ServletUtilities;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;

public class RegistrationServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        HttpSession session = req.getSession();

        if ((session.getAttribute("user_a") == null)){

            Cookie cookie = ServletUtilities.getCookie(req, "remember");

            if (cookie != null){
                //достаем user'а из БД с таким же cookie
                try {
                    User user = UserRepository.getUserByCookie(cookie);
                    if (user !=null){
                        //меняем значение cookie для безопасности
                        Cookie newCookie = new Cookie("remember", SecurityService.genRndHash(12));
                        newCookie.setMaxAge(60*60*48);
                        UserRepository.updateUserCookie(user,newCookie);
                        resp.addCookie(newCookie);
                        session.setAttribute("user_a",user);
                        resp.sendRedirect("/welcome");
                        return;
                    }

                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            getServletContext().getRequestDispatcher("/WEB-INF/views/registration.jsp").forward(req,resp);
        }else {
            resp.sendRedirect("/welcome");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        //вытаскиваем информацию формы
        String email = req.getParameter("email");
        String password = req.getParameter("password");
        String sex = req.getParameter("sex");
        String subscription = req.getParameter("subscription") == null ? "off":"on";
        String about = req.getParameter("about");


        //ajax checking an email for free
        String param = req.getParameter("ch_email");
        if (param != null){
            try {
                resp.setCharacterEncoding("utf-8");
                resp.setContentType("application/json");

                UserRepository.checkUserEmail(param);

                if (UserRepository.getUserByEmail(param) != null){
                    resp.getWriter().write("{\"param\" : \"1\"}");
                }else {
                    resp.getWriter().write("{\"param\" : \"0\"}");
                }

            } catch (SQLException e) {
                e.printStackTrace();
            } catch (NotValidEmailException e) {
                resp.getWriter().write("{\"param\" : \"1\"}");
            }
            return;
        }

        if (sex == null || "".equals(email) || "".equals(password) ){

            req.setAttribute("message","Fill all fields, please!");

        }else {

            //пытаемся добавить пользователя
            try{

                UserRepository.addUser( new User(email,password,sex,subscription,about) );
                resp.sendRedirect("/login?status=ok");
                return;

            } catch (NotValidPasswordException | NotValidEmailException | DatabaseException e) {
                req.setAttribute("message",e.getMessage());

            } catch (SQLException e) {
                //duplicate sql error code
                if (e.getErrorCode() == 1062){
                    req.setAttribute("message","User already exists");
                }
            } catch (SecurityException e) {
                req.setAttribute("message","some problems in server(");
                e.printStackTrace();
            }
        }
        getServletContext().getRequestDispatcher("/WEB-INF/views/registration.jsp").forward(req,resp);
    }
}
