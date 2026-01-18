//
//  EnvironmentConfig.swift
//  iosApp
//
//  Created by Jordi Bujaldon on 18/1/26.
//

import Shared

class EnvironmentConfig {
    private let configKey = "EnvironmentConfiguration"

    private enum BuildEnvironment: String {
        case prod = "prod"
        case dev = "dev"
    }

    static func getBuildEnvironment() -> CoreBuildEnvironment {
        let config = Bundle.main.infoDictionary?[String(describing: BuildEnvironment.self)] as? String ?? "dev"
        switch config {
        case BuildEnvironment.dev.rawValue:
            return .dev
        case BuildEnvironment.prod.rawValue:
            return .prod
        default:
            return .dev
        }
    }
}
