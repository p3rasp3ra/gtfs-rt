package com.marszrut.gtfs_rt.dto;

//basically VehiclePosition - feedId, routeId, tripId, direction
public record VPDto(String vid,
                    String vlp,
                    String vl,
                    String sd,
                    String st,
                    Double lat,
                    Double lon,
                    String t,
                    String os,
                    String cs,
                    String css,
                    Integer did,
                    String aid

) {
}
