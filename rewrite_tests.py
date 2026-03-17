import re
import os

def rewrite_controller(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    # Revert any existing Arrange
    content = content.replace('\n        // Arrange', '')

    tests = [
        ("testGetBidById_returnsBid", 
         "when(bidService.getBidById(\"bid-1\")).thenReturn(testBid1);", 
         "mockMvc.perform(get(\"/api/bids/{id}\", \"bid-1\")"),
         
        ("testGetBidsForProject_returnsList", 
         "List<BidResponse> bids = Arrays.asList(testBid1, testBid2);", 
         "mockMvc.perform(get(\"/api/bids\")"),
         
        ("testSubmitBid_createsBid", 
         "CreateBidRequest request = new CreateBidRequest(\"project-1\", \"freelancer-1\", 100.0, \"Message 1\");", 
         "mockMvc.perform(post(\"/api/bids\")"),
         
        ("testAcceptBid_returnsAcceptedBid", 
         "when(bidService.acceptBid(\"bid-2\")).thenReturn(testBid2);", 
         "mockMvc.perform(patch(\"/api/bids/{id}/accept\", \"bid-2\")"),
         
        ("testRejectBid_returnsRejectedBid", 
         "BidResponse rejectedBid = new BidResponse(\"bid-1\", \"project-1\", \"freelancer-1\", 100.0, \"Message 1\", BidStatus.REJECTED);", 
         "mockMvc.perform(patch(\"/api/bids/{id}/reject\", \"bid-1\")"),
    ]

    for name, arrange_code, act_code in tests:
        # insert // Arrange
        content = content.replace(f"    void {name}() throws Exception {{\n        {arrange_code}", 
                                  f"    void {name}() throws Exception {{\n        // Arrange\n        {arrange_code}")
        # Insert // Act & Assert
        content = content.replace(f"\n        {act_code}", 
                                  f"\n\n        // Act & Assert\n        {act_code}")

    with open(filepath, 'w') as f:
        f.write(content)

rewrite_controller("src/test/java/ro/unibuc/prodeng/controller/BidControllerTest.java")

print("Done Controller!")
