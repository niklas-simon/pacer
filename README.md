# Pacer

An Android App for automatically adding a specified amount steps to Health Connect each day while making it look like you actually walked.

![Home Screen](/docs/home.png)

## Requirements

- Android 14+

## Usage

1. Install App
2. Open App
3. Allow required permissions
    - Read/write steps: needed because that's the intended use of the app
    - Read in background: background task checks other sources (like Samsung Health) to account for steps you actually took
4. Configure App
    - Target: steps you want each day (default: 10,000)
    - Active range: time range in between which steps should be added (default: 06:00-22:00)
5. Set Priority
    - Go to Settings > Security and privacy > Privacy > Health Connect > Data and access > Activity > Data sources and priority
    - Under App sources, move Pacer to the top