package com.project.shopapp.repositories;

import com.project.shopapp.models.TableReservation;
import com.project.shopapp.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TableReservationRepository extends JpaRepository<TableReservation, Long> {
    @Query("SELECT t FROM TableReservation t WHERE t.customer = :customer ORDER BY t.createTime DESC")
    List<TableReservation> findByCustomerOrderByCreateTimeDesc(@Param("customer") User customer);

    boolean existsByReservationCode(String reservationCode);

    @Query("SELECT tr FROM TableReservation tr WHERE tr.reservationCode LIKE %:code%")
    List<TableReservation> findByReservationCodeLike(@Param("code") String code);

    // don hang thanh cong trong khoang thoi gian
    @Query("SELECT COUNT(tr) FROM TableReservation tr WHERE tr.status = 'SUCCESS' AND tr.createTime BETWEEN :startDate AND :endDate")
    Long countSuccessTableReservationByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // don hang huy trong khoang thoi gian
    @Query("SELECT COUNT(tr) FROM TableReservation tr WHERE tr.status = 'CANCELLED' AND tr.createTime BETWEEN :startDate AND :endDate")
    Long countCanceledOrdersByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
