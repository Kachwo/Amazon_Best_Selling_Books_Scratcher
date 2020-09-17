/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kcwong373.amazonbestsellingbooks;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

/**
 *
 * @author wkchun
 */
@WebServlet(name = "BooksServlet", urlPatterns = {"/BooksServlet"})
public class Servlet extends HttpServlet {

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SQLException{
        
        Connection con = null;
        PreparedStatement prepst = null;
        Statement st = null;
        Statement st2 = null;
        Savepoint save = null;
        ResultSet rs = null;
        ResultSet rs2 = null;
        Timestamp old = null;
        Timestamp now = new Timestamp(System.currentTimeMillis());
        Boolean empty = false;
        String url = "jdbc:derby://localhost:1527/BestSellingBooks";
        String user = "test";
        String password = "test";

        
        con = DriverManager.getConnection(url, user, password);
        con.setAutoCommit(false);
        st = con.createStatement();
        

        rs = st.executeQuery("SELECT * FROM Books");
        if (rs.next()) {
                
            old = rs.getTimestamp("Modtime");
                
        } else {
                
            empty = true;
                
        }
        if (empty || (now.getTime() - old.getTime() > 120000)){
                
            Document doc = Jsoup
                    .connect("https://www.amazon.com/gp/bestsellers/books/")
                    .validateTLSCertificates(false)
                    .userAgent("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/48.0.2564.116 Safari/537.36")
                    .get();

            Document doc2 = Jsoup
                    .connect("https://www.amazon.com/best-sellers-books-Amazon/zgbs/books/ref=zg_bs_pg_2?_encoding=UTF8&pg=2")
                    .validateTLSCertificates(false)
                    .userAgent("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/48.0.2564.116 Safari/537.36")
                    .get();
        
            Elements covers = doc.select("div.a-section > img");
            Elements covers2 = doc2.select("div.a-section > img");
            Elements rankings = doc.select("div.a-row span.zg-badge-text"); 
            Elements rankings2 = doc2.select("div.a-row span.zg-badge-text"); 
            Elements titles = doc.select("div.p13n-sc-truncate");
            Elements titles2 = doc2.select("div.p13n-sc-truncate");
            Elements authors = doc.select("div.a-size-small > a.a-size-small,span.a-size-small.a-color-base");
            Elements authors2 = doc2.select("div.a-size-small > a.a-size-small,span.a-size-small.a-color-base");
            Elements prices = doc.select("span.p13n-sc-price");
            Elements prices2 = doc2.select("span.p13n-sc-price");

            st.executeUpdate("DELETE FROM Books");
            Timestamp modTime = new Timestamp(System.currentTimeMillis());
            prepst = con.prepareStatement("INSERT INTO Books (Cover, Ranking, Title, Author, Price, Modtime)" + "VALUES (?,?,?,?,?,?)");
        
            for (int i=0;i<50;i=i+1){
                try {
                        
                    save = con.setSavepoint();  

                    prepst.setString(1,covers.get(i).attr("src"));
                    prepst.setString(2,rankings.get(i).text());
                    prepst.setString(3,titles.get(i).text());
                    prepst.setString(4,authors.get(i).text());
                    prepst.setString(5,prices.get(i).text());
                    prepst.setTimestamp(6,modTime);
                    prepst.execute();
                
                } catch (SQLException ex) {
                        
                    Logger.getLogger(Servlet.class.getName()).log(Level.SEVERE, ex.getMessage());
                    con.rollback(save);
                        
                } finally {
                        
                    con.releaseSavepoint(save);
                    con.commit();
                        
                }
            }
        
            for (int i=0;i<50;i=i+1){
                try {
                        
                    save = con.setSavepoint();  

                    prepst.setString(1,covers2.get(i).attr("src"));
                    prepst.setString(2,rankings2.get(i).text());
                    prepst.setString(3,titles2.get(i).text());
                    prepst.setString(4,authors2.get(i).text());
                    prepst.setString(5,prices2.get(i).text());
                    prepst.execute();

                } catch (SQLException ex) {
                        
                    Logger.getLogger(Servlet.class.getName()).log(Level.SEVERE, ex.getMessage());
                    con.rollback(save);

                } finally {

                    con.releaseSavepoint(save);
                    con.commit();
                        
                }
            }
        }   
           
        st.close();
        prepst.close();
        rs.close();
        

        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter output = response.getWriter()) {
            
            output.println("<html>");
            output.println("<head>");
            output.println("<title>Top 100 Best Seller</title>");
            output.println("<link rel=\"stylesheet\" type=\"text/css\" href=\"" + request.getContextPath() + "\\styles\\style.css\">");
            output.println("</head>");
            output.println("<div class=\"header\"><h1>Top 100 Best Seliing Books in Amazon</h1></div>");
            output.println("<body>");

            ArrayList coverList = new ArrayList();
            ArrayList rankingList = new ArrayList();
            ArrayList titleList = new ArrayList();
            ArrayList authorList = new ArrayList();
            ArrayList priceList = new ArrayList();
            
            st2 = con.createStatement();
            rs2 = st2.executeQuery("SELECT * FROM Books");

            while (rs2.next()) {
                
                coverList.add(rs2.getString("Cover"));
                rankingList.add(rs2.getString("Ranking"));
                titleList.add(rs2.getString("Title"));
                authorList.add(rs2.getString("Author"));
                priceList.add(rs2.getString("Price"));

                output.println("<div class=\"ranking\"><h2>" + rs2.getString("Ranking") + "</h2></div>");
                output.println("<center><div class=\"cover\"><img src=\"" + rs2.getString("Cover") + "\"></center>");
                output.println("<br>");
                output.println("<div class=\"title\"><h4>Title: " + rs2.getString("Title") + "</h4></div>");
                output.println("<div class=\"author\"><h4>Author: " + rs2.getString("Author") + "</h4></div>");
                output.println("<div class=\"price\"><h4>Price: " + rs2.getString("Price") +  "</h4></div>");
                output.println("<br><hr>");
                
            }
            output.println("</body>");
            output.println("</html>");
            
            output.close();
            rs2.close();
            con.close();
            
        } catch (Exception e) { 
            e.printStackTrace(); 
        } 
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try{
            processRequest(request, response);
        } catch (SQLException ex) {
            Logger.getLogger(Servlet.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try{
            processRequest(request, response);
        } catch (SQLException ex) {
            Logger.getLogger(Servlet.class.getName()).log(Level.SEVERE,null, ex);
        }
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
