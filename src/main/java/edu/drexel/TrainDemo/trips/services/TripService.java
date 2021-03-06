package edu.drexel.TrainDemo.trips.services;

import edu.drexel.TrainDemo.trips.models.Itinerary;
import edu.drexel.TrainDemo.trips.models.Segment;
import edu.drexel.TrainDemo.trips.models.TripSearchRequest;
import edu.drexel.TrainDemo.trips.models.entities.StationEntity;
import edu.drexel.TrainDemo.trips.models.entities.StopTimeEntity;
import edu.drexel.TrainDemo.trips.models.entities.TripEntity;
import edu.drexel.TrainDemo.trips.repositories.StationRepository;
import edu.drexel.TrainDemo.trips.repositories.TripRepository;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TripService {
    Logger logger = LoggerFactory.getLogger(TripService.class);
    private StationRepository stationRepository;
    private TripRepository tripRepository;

    public TripService(StationRepository stationRepository, TripRepository tripRepository) {
        this.stationRepository = stationRepository;
        this.tripRepository = tripRepository;
    }

    public List<Itinerary> getMatchingTrips(TripSearchRequest searchRequest) {
        String toId = searchRequest.getTo();
        String fromId = searchRequest.getFrom();

        StationEntity toStation = safeGetStationFromId(toId);
        StationEntity fromStation = safeGetStationFromId(fromId);

        List<GraphPath<StationEntity, StopTimeEntityEdge>> pathList = searchGraph(fromStation, toStation);

        List<Itinerary> itineraryList = new ArrayList<>();
        for (GraphPath<StationEntity, StopTimeEntityEdge> path : pathList) {
            List<StopTimeEntityEdge> edgeList = path.getEdgeList();

            List<Segment> segments = new ArrayList<>();
            for (StopTimeEntityEdge edge : edgeList) {
                StopTimeEntity fromStop = edge.getFrom();
                StopTimeEntity toStop = edge.getTo();
                segments.add(new Segment(fromStop, toStop));
            }

            itineraryList.add(new Itinerary(segments));
        }
        List<Itinerary> validItineraries = itineraryList.stream().filter(itinerary -> itinerary.isValid()).collect(Collectors.toList());
        return validItineraries;
    }

    public List<GraphPath<StationEntity, StopTimeEntityEdge>> searchGraph(StationEntity fromStation, StationEntity toStation) {
        logger.info("Constructing graph...");
        long start = System.currentTimeMillis();
        Graph<StationEntity, StopTimeEntityEdge> g = createGraph();
        long end = System.currentTimeMillis();
        logger.info(String.format("Completed constructing graph after %dms", end - start));
        DijkstraShortestPath<StationEntity, StopTimeEntityEdge> searchAlgorithm =
                new DijkstraShortestPath<>(g);

        logger.info("Searching using Dijkstra to get shortest path length...");
        start = System.currentTimeMillis();
        GraphPath<StationEntity, StopTimeEntityEdge> shortestPath = searchAlgorithm.getPath(fromStation, toStation);
        if (shortestPath == null) {
            return new ArrayList<>();
        }
        int maxPathLength = shortestPath.getLength();
        end = System.currentTimeMillis();
        logger.info(String.format("Found shortest path with length %d after %d ms", maxPathLength, end - start));
        AllDirectedPaths<StationEntity, StopTimeEntityEdge> allDirectedPathsAlgorithm = new AllDirectedPaths<>(g);
        logger.info("Finding all paths ...");
        start = System.currentTimeMillis();
        List<GraphPath<StationEntity, StopTimeEntityEdge>> iPaths = allDirectedPathsAlgorithm.getAllPaths(fromStation, toStation, true, maxPathLength + 1);
        end = System.currentTimeMillis();
        logger.info(String.format("Found %d paths after %d ms", iPaths.size(), end - start));
        return iPaths;

    }

    public Graph<StationEntity, StopTimeEntityEdge> createGraph() {
        // TODO make static
        Graph<StationEntity, StopTimeEntityEdge> g = new SimpleDirectedGraph<>(StopTimeEntityEdge.class);
        for (TripEntity trip : tripRepository.findAll()) {
            List<StopTimeEntity> stops = trip.getStops();
            stops.stream().forEach(stop -> {
                g.addVertex(stop.getStation());
            });
            int numStops = stops.size();
            for (int i = 0; i < numStops - 1; i++) {
                for (int j = i + 1; j < numStops - 1; j++) {
                    addEdge(g, stops.get(i), stops.get(j));
                }
            }
        }
        return g;
    }

    private void addEdge(Graph<StationEntity, StopTimeEntityEdge> g, StopTimeEntity outbound, StopTimeEntity inbound) {
        g.addEdge(outbound.getStation(), inbound.getStation(), new StopTimeEntityEdge(outbound, inbound));
    }

    public Itinerary constructItinerary(Itinerary unsafeItinerary) {
        List<Segment> segments = new ArrayList<>();
        for (Segment unsafeSegment : unsafeItinerary.getSegments()) {
            Segment safeSegment = findSegment(unsafeSegment);
            segments.add(safeSegment);
        }
        return new Itinerary(segments);
    }

    public Segment findSegment(Segment segment) {
        return findSegment(segment.getTrip().getId(), segment.getFrom().getId(), segment.getTo().getId(), segment.getDeparture(), segment.getArrival());
    }

    public Segment findSegment(Long tripId, String fromId, String toId, Time departure, Time arrival) {
        Itinerary itinerary = findItinerary(tripId, fromId, toId, departure, arrival);
        Segment segment = itinerary.segments.get(0);
        return segment;
    }

    public Itinerary findItinerary(Long tripId, String fromId, String toId, Time departure, Time arrival) {
        Optional<TripEntity> tripResult = tripRepository.findById(tripId);
        if (!tripResult.isPresent()) {
            throw new IllegalArgumentException();
        }

        TripEntity trip = tripResult.get();

        Optional<StopTimeEntity> fromStopResult = trip.getStops().stream().filter(stop -> stop.getStation().getId().equals(fromId)).findFirst();
        Optional<StopTimeEntity> toStopResult = trip.getStops().stream().filter(stop -> stop.getStation().getId().equals(toId)).findFirst();

        if (!fromStopResult.isPresent() || !toStopResult.isPresent()) {
            throw new IllegalArgumentException();
        }

        StopTimeEntity fromStop = fromStopResult.get();
        StopTimeEntity toStop = toStopResult.get();

        if (!fromStop.getDepartureTime().equals(departure) || !toStop.getArrivalTime().equals(arrival)) {
            throw new IllegalArgumentException();
        }

        return new Itinerary(trip, fromStop, toStop);
    }

    private StationEntity safeGetStationFromId(String id) {
        //TODO Add to document that we are making sure data is clean/valid
        Optional<StationEntity> result = stationRepository.findById(id);

        if (!result.isPresent()) {
            throw new IllegalArgumentException();
        }

        return result.get();
    }

    public Iterable<StationEntity> getAllStations() {
        return stationRepository.findAll();
    }
}
