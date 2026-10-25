import ActivityKit
import AlarmKit
import AppIntents
import Foundation
import KotlinModules
import SwiftUI

/// What an alarm remembers about its task.
@available(iOS 26.0, *)
struct TaskAlarmMetadata: AlarmMetadata {
    var entryId: String
}

/// The alarm's Open task button: opens the app on the task, through the same request a tapped notification makes.
@available(iOS 26.0, *)
struct OpenTaskIntent: LiveActivityIntent {
    static var title: LocalizedStringResource = "Open task"
    static var openAppWhenRun: Bool = true

    @Parameter(title: "Task")
    var entryId: String

    init() {}

    init(entryId: String) {
        self.entryId = entryId
    }

    func perform() async throws -> some IntentResult {
        IosBridge.shared.entryRequests.open(entryId: entryId)
        return .result()
    }
}

/// One alarm time, copied out of the Kotlin objects.
struct AlarmTime: Sendable {
    let entryId: String
    let text: String
    let date: Date
}

/// Offers AlarmKit to the Kotlin side. AlarmKit exists from iOS 26, so everything is guarded by availability.
final class AlarmKitBridge: NSObject, IosAlarmBridge {
    var isAvailable: Bool {
        if #available(iOS 26.0, *) { return true }
        return false
    }

    func isAuthorized() -> Bool {
        guard #available(iOS 26.0, *) else { return false }
        return AlarmManager.shared.authorizationState == .authorized
    }

    func requestAuthorization(onResult: @escaping (KotlinBoolean) -> Void) {
        guard #available(iOS 26.0, *) else {
            onResult(KotlinBoolean(bool: false))
            return
        }
        Task {
            let state = try? await AlarmManager.shared.requestAuthorization()
            onResult(KotlinBoolean(bool: state == .authorized))
        }
    }

    func replaceAll(items: [IosAlarmItem]) {
        guard #available(iOS 26.0, *) else { return }
        let times = items.map {
            AlarmTime(entryId: $0.entryId, text: $0.text, date: Date(timeIntervalSince1970: TimeInterval($0.atMillis) / 1000))
        }
        Task { await AlarmScheduler.shared.enqueue(times) }
    }
}

/// Applies each new set of alarms after the previous one has finished, so two quick changes cannot interleave.
@available(iOS 26.0, *)
actor AlarmScheduler {
    static let shared = AlarmScheduler()
    private var tail: Task<Void, Never>?

    func enqueue(_ times: [AlarmTime]) {
        let previous = tail
        tail = Task {
            await previous?.value
            await AlarmScheduler.apply(times)
        }
    }

    /// Cancels the app's alarms that are not ringing now, then schedules one alarm for each time still ahead.
    private static func apply(_ times: [AlarmTime]) async {
        let manager = AlarmManager.shared
        guard manager.authorizationState == .authorized else { return }
        for alarm in (try? manager.alarms) ?? [] where alarm.state != .alerting {
            try? manager.cancel(id: alarm.id)
        }
        for time in times where time.date > Date() {
            let button = AlarmButton(text: "Open task", textColor: .white, systemImageName: "arrow.up.forward.app")
            let title = LocalizedStringResource("\(time.text)")
            let alert: AlarmPresentation.Alert
            if #available(iOS 26.1, *) {
                alert = AlarmPresentation.Alert(title: title, secondaryButton: button, secondaryButtonBehavior: .custom)
            } else {
                alert = AlarmPresentation.Alert(title: title, stopButton: button, secondaryButton: button, secondaryButtonBehavior: .custom)
            }
            let attributes = AlarmAttributes(
                presentation: AlarmPresentation(alert: alert),
                metadata: TaskAlarmMetadata(entryId: time.entryId),
                tintColor: .orange
            )
            let configuration = AlarmManager.AlarmConfiguration<TaskAlarmMetadata>.alarm(
                schedule: .fixed(time.date),
                attributes: attributes,
                stopIntent: nil,
                secondaryIntent: OpenTaskIntent(entryId: time.entryId),
                sound: .default
            )
            _ = try? await manager.schedule(id: UUID(), configuration: configuration)
        }
    }
}
