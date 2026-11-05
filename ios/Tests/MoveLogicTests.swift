import XCTest
@testable import DamovesLogic

final class MoveLogicTests: XCTestCase {
    func testNamesAndExtensions() {
        XCTAssertEqual(MoveLogic.name(for: URL(fileURLWithPath: "cross_body_lead.mp4")), "Cross body lead")
        XCTAssertTrue(MoveLogic.videoExtensions.contains("mov"))
        XCTAssertFalse(MoveLogic.videoExtensions.contains("xml"))
    }
    func testSearch() {
        XCTAssertTrue(MoveLogic.matches("Cross body lead", query: " BODY "))
        XCTAssertTrue(MoveLogic.matches("Cross body lead", query: " \n"))
        XCTAssertFalse(MoveLogic.matches("Cross body lead", query: "turn"))
    }
    func testRangeClampsAndNeverInverts() {
        var range = ClipRange(duration: 10)
        range.setStart(-1)
        XCTAssertEqual(range.start, 0)
        range.setEnd(50)
        XCTAssertEqual(range.end, 10)
        range.setStart(20)
        XCTAssertLessThan(range.start, range.end)
        range.setEnd(0)
        XCTAssertGreaterThan(range.end, range.start)
    }
}
