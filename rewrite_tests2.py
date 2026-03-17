import re
import os

def rewrite_service(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    # Pattern matches roughly a test method definition, followed by some code before assertThrows or before assert/verify, or before method call to be assigned
    
    # Simple regex replacing for specific ones
    
    content = content.replace('    void getBidById_existingBid_returnsBid() {\n        BidEntity bid',
                              '    void getBidById_existingBid_returnsBid() {\n        // Arrange\n        BidEntity bid')
    content = content.replace('        BidResponse result = bidService.getBidById("bid1");',
                              '\n        // Act\n        BidResponse result = bidService.getBidById("bid1");\n\n        // Assert')
                              
    content = content.replace('    void getBidById_nonExistingBid_throwsEntityNotFoundException() {\n        when',
                              '    void getBidById_nonExistingBid_throwsEntityNotFoundException() {\n        // Arrange\n        when')
    content = content.replace('        assertThrows(EntityNotFoundException',
                              '\n        // Act & Assert\n        assertThrows(EntityNotFoundException')
                              
    content = content.replace('    void submitBid_validRequest_savesAndReturnsBid() {\n        when',
                              '    void submitBid_validRequest_savesAndReturnsBid() {\n        // Arrange\n        when')
    content = content.replace('        BidResponse response = bidService.submitBid(validRequest);',
                              '\n        // Act\n        BidResponse response = bidService.submitBid(validRequest);\n\n        // Assert')
                              
    content = content.replace('    void submitBid_projectNotOpen_throwsIllegalArgumentException() {\n        ProjectEntity closedProject',
                              '    void submitBid_projectNotOpen_throwsIllegalArgumentException() {\n        // Arrange\n        ProjectEntity closedProject')
    content = content.replace('        assertThrows(IllegalArgumentException',
                              '\n        // Act & Assert\n        assertThrows(IllegalArgumentException')
                              
    content = content.replace('    void submitBid_duplicateBid_throwsIllegalArgumentException() {\n        when',
                              '    void submitBid_duplicateBid_throwsIllegalArgumentException() {\n        // Arrange\n        when')

    content = content.replace('    void submitBid_skillMismatch_throwsIllegalArgumentException() {\n        FreelancerEntity',
                              '    void submitBid_skillMismatch_throwsIllegalArgumentException() {\n        // Arrange\n        FreelancerEntity')
                              
    content = content.replace('    void submitBid_freelancerAtMaxCapacity_throwsIllegalArgumentException() {\n        when',
                              '    void submitBid_freelancerAtMaxCapacity_throwsIllegalArgumentException() {\n        // Arrange\n        when')
                              
    content = content.replace('    void acceptBid_pendingBidOnOpenProject_acceptsAndCascades() {\n        BidEntity bid',
                              '    void acceptBid_pendingBidOnOpenProject_acceptsAndCascades() {\n        // Arrange\n        BidEntity bid')
    content = content.replace('        BidResponse result = bidService.acceptBid("bid1");',
                              '\n        // Act\n        BidResponse result = bidService.acceptBid("bid1");\n\n        // Assert')

    content = content.replace('    void acceptBid_projectNotOpen_throwsIllegalArgumentException() {\n        BidEntity bid',
                              '    void acceptBid_projectNotOpen_throwsIllegalArgumentException() {\n        // Arrange\n        BidEntity bid')

    content = content.replace('    void acceptBid_alreadyAccepted_throwsIllegalArgumentException() {\n        BidEntity acceptedBid',
                              '    void acceptBid_alreadyAccepted_throwsIllegalArgumentException() {\n        // Arrange\n        BidEntity acceptedBid')

    content = content.replace('    void acceptBid_bidNotFound_throwsEntityNotFoundException() {\n        when',
                              '    void acceptBid_bidNotFound_throwsEntityNotFoundException() {\n        // Arrange\n        when')
                              
    content = content.replace('    void rejectBid_pendingBid_rejectsSuccessfully() {\n        BidEntity bid',
                              '    void rejectBid_pendingBid_rejectsSuccessfully() {\n        // Arrange\n        BidEntity bid')
    content = content.replace('        BidResponse result = bidService.rejectBid("bid1");',
                              '\n        // Act\n        BidResponse result = bidService.rejectBid("bid1");\n\n        // Assert')

    content = content.replace('    void rejectBid_alreadyRejected_throwsIllegalArgumentException() {\n        BidEntity rejectedBid',
                              '    void rejectBid_alreadyRejected_throwsIllegalArgumentException() {\n        // Arrange\n        BidEntity rejectedBid')
                              
    content = content.replace('    void getBidsForProject_existingProject_returnsBids() {\n        BidEntity bid',
                              '    void getBidsForProject_existingProject_returnsBids() {\n        // Arrange\n        BidEntity bid')
    content = content.replace('        List<BidResponse> results = bidService.getBidsForProject("pid");',
                              '\n        // Act\n        List<BidResponse> results = bidService.getBidsForProject("pid");\n\n        // Assert')


    with open(filepath, 'w') as f:
        f.write(content)

rewrite_service("src/test/java/ro/unibuc/prodeng/service/BidServiceTest.java")

print("Done Service!")
