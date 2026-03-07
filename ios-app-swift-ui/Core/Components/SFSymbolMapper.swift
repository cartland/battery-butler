import Foundation

enum SFSymbolMapper {
    static func sfSymbolName(for iconName: String?) -> String {
        guard let iconName = iconName else { return "desktopcomputer" }
        return mapping[iconName] ?? "desktopcomputer"
    }

    private static let mapping: [String: String] = [
        // Shapes
        "star": "star.fill",
        "circle": "circle.fill",
        "square": "square.fill",
        "favorite": "heart.fill",
        "diamond": "diamond.fill",
        "hexagon": "hexagon.fill",

        // Electronics
        "smartphone": "iphone",
        "tablet": "ipad",
        "laptop": "laptopcomputer",
        "watch": "applewatch",
        "headphones": "headphones",
        "camera": "camera.fill",
        "speaker": "hifispeaker.fill",
        "videogame_asset": "gamecontroller.fill",
        "game_controller": "gamecontroller.fill",
        "tv": "tv",
        "router": "wifi.router",
        "power": "bolt.fill",
        "smart_button": "button.programmable",
        "settings_remote": "av.remote.fill",
        "mouse": "computermouse.fill",
        "keyboard": "keyboard",

        // Home / Smart Home
        "lightbulb": "lightbulb.fill",
        "detector_smoke": "sensor.fill",
        "thermostat": "thermometer.medium",
        "sensors": "sensor.fill",
        "lock": "lock.fill",
        "garage_home": "door.garage.closed",

        // Tools / Utility
        "flashlight_on": "flashlight.on.fill",
        "drill": "wrench.and.screwdriver.fill",
        "brush": "paintbrush.fill",
        "scale": "scalemass.fill",
        "straighten": "ruler.fill",
        "water_drop": "drop.fill",

        // Other
        "car": "car.fill",
        "bike": "bicycle",
        "schedule": "clock.fill",
        "location_on": "location.fill",
        "account_balance_wallet": "creditcard.fill",
        "toys": "teddybear.fill",
    ]
}
