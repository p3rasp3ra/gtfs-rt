package com.marszrut.gtfs_rt.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * Domain model representing a GTFS-RT vehicle position.
 * This class contains all the information about a vehicle's current position and status.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "vehicle_positions")
public class VehiclePosition implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vehicle_id", nullable = false)
    private String vid;

    @Column(name = "latitude", nullable = false)
    private Double lat;

    @Column(name = "longitude", nullable = false)
    private Double lon;

    @Column(name = "timestamp", nullable = false)
    private Instant t;

    @Column(name = "feed_id", nullable = false)
    private String fid;

    @Column(name = "agency_id", nullable = false)
    private String aid;

    @Column(name = "route_id", nullable = false)
    private String rid;

    @Column(name = "trip_id", nullable = false)
    private String tid;

    @Column(name = "direction_id", nullable = false)
    private Integer did;

    @Column(name = "start_date", nullable = false)
    private String sd;

    @Column(name = "start_time", nullable = false)
    private String st;

    @Column(name = "current_stop_id", nullable = false)
    private String sid;

    //0 - in transit to, 1 - stopped at
    @Column(name = "current_stop_status", nullable = false)
    private int ss;

    @Column(name = "vehicle_label", nullable = false)
    private String vl;

    @Column(name = "license_plate", nullable = false)
    private String lp;

    @Column(name = "occupancy_status", nullable = false)
    private Integer os;

    public static VehiclePositionBuilder builder() {
        return new VehiclePositionBuilder();
    }

    public static class VehiclePositionBuilder {
        private Long id;
        private String vid;
        private Double lat;
        private Double lon;
        private Instant t;
        private String fid;
        private String aid;
        private String rid;
        private String tid;
        private Integer did;
        private String sd;
        private String st;
        private String sid;
        private int ss;
        private String vl;
        private String lp;
        private Integer os;

        VehiclePositionBuilder() {
        }

        public VehiclePositionBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public VehiclePositionBuilder vid(String vid) {
            this.vid = vid;
            return this;
        }

        public VehiclePositionBuilder lat(Double lat) {
            this.lat = lat;
            return this;
        }

        public VehiclePositionBuilder lon(Double lon) {
            this.lon = lon;
            return this;
        }

        public VehiclePositionBuilder t(Instant t) {
            this.t = t;
            return this;
        }

        public VehiclePositionBuilder fid(String fid) {
            this.fid = fid;
            return this;
        }

        public VehiclePositionBuilder aid(String aid) {
            this.aid = aid;
            return this;
        }

        public VehiclePositionBuilder rid(String rid) {
            this.rid = rid;
            return this;
        }

        public VehiclePositionBuilder tid(String tid) {
            this.tid = tid;
            return this;
        }

        public VehiclePositionBuilder did(Integer did) {
            this.did = did;
            return this;
        }

        public VehiclePositionBuilder sd(String sd) {
            this.sd = sd;
            return this;
        }

        public VehiclePositionBuilder st(String st) {
            this.st = st;
            return this;
        }

        public VehiclePositionBuilder sid(String sid) {
            this.sid = sid;
            return this;
        }

        public VehiclePositionBuilder ss(int ss) {
            this.ss = ss;
            return this;
        }

        public VehiclePositionBuilder vl(String vl) {
            this.vl = vl;
            return this;
        }

        public VehiclePositionBuilder lp(String lp) {
            this.lp = lp;
            return this;
        }

        public VehiclePositionBuilder os(Integer os) {
            this.os = os;
            return this;
        }

        public VehiclePosition build() {
            return new VehiclePosition(this.id, this.vid, this.lat, this.lon, this.t, this.fid, this.aid, this.rid, this.tid, this.did, this.sd, this.st, this.sid, this.ss, this.vl, this.lp, this.os);
        }

        public String toString() {
            return "VehiclePosition.VehiclePositionBuilder(id=" + this.id + ", vid=" + this.vid + ", lat=" + this.lat + ", lon=" + this.lon + ", t=" + this.t + ", fid=" + this.fid + ", aid=" + this.aid + ", rid=" + this.rid + ", tid=" + this.tid + ", did=" + this.did + ", sd=" + this.sd + ", st=" + this.st + ", sid=" + this.sid + ", ss=" + this.ss + ", vl=" + this.vl + ", lp=" + this.lp + ", os=" + this.os + ")";
        }
    }
}
