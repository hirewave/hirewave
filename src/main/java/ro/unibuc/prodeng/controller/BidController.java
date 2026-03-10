package ro.unibuc.prodeng.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import ro.unibuc.prodeng.request.CreateBidRequest;
import ro.unibuc.prodeng.response.BidResponse;
import ro.unibuc.prodeng.service.BidService;

@RestController
@RequestMapping("/api/bids")
public class BidController {

    @Autowired
    private BidService bidService;

    @GetMapping("/{id}")
    public ResponseEntity<BidResponse> getBidById(@PathVariable String id) {
        return ResponseEntity.ok(bidService.getBidById(id));
    }

    @GetMapping
    public ResponseEntity<List<BidResponse>> getBidsForProject(@RequestParam String projectId) {
        return ResponseEntity.ok(bidService.getBidsForProject(projectId));
    }

    @PostMapping
    public ResponseEntity<BidResponse> submitBid(@Valid @RequestBody CreateBidRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bidService.submitBid(request));
    }

    @PatchMapping("/{id}/accept")
    public ResponseEntity<BidResponse> acceptBid(@PathVariable String id) {
        return ResponseEntity.ok(bidService.acceptBid(id));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<BidResponse> rejectBid(@PathVariable String id) {
        return ResponseEntity.ok(bidService.rejectBid(id));
    }
}
