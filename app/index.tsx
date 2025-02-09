import * as SplashScreen from 'expo-splash-screen';
import { useFonts } from 'expo-font';
import { useEffect, useState } from 'react';
import { RecordResult } from "react-native-health-connect";
import { readSteps } from "@/api/health";
import { load, save } from '@/api/storage';
import React from 'react';
import { Text, Button, TextInput, Card, useTheme, ProgressBar } from "react-native-paper";
import { TimePickerModal } from "react-native-paper-dates";
import { StatusBar, View } from 'react-native';
import { configure, start, status, TaskInfo } from '@/api/background';
import BackgroundFetch, { BackgroundFetchStatus } from 'react-native-background-fetch';

export interface Options {
    target: number,
    from: string,
    to: string
}

interface Source {
    app: string,
    steps: number
}

SplashScreen.preventAutoHideAsync();
configure().then(start);

function dateToString(date: Date) {
    const day = date.getDate().toString().padStart(2, "0");
    const month = (date.getMonth() + 1).toString().padStart(2, "0");
    const year = date.getFullYear().toString();
    const hour = date.getHours().toString().padStart(2, "0");
    const minute = date.getMinutes().toString().padStart(2, "0");
    return `${day}.${month}.${year} ${hour}:${minute}`;
}

export default function Index() {
    const [loaded] = useFonts({
        SpaceMono: require('../assets/fonts/SpaceMono-Regular.ttf'),
    });

    const theme = useTheme();

    const [stepData, setStepData] = useState<RecordResult<"Steps">[] | null>(null);

    const [options, setOptions] = useState<Options | null>(null);
    const [taskInfo, setTaskInfo] = useState<TaskInfo | null>(null);
    const [taskStatus, setTaskStatus] = useState<BackgroundFetchStatus | null>(null);
    const [fromPickerVisible, setFromPickerVisible] = useState(false);
    const [toPickerVisible, setToPickerVisible] = useState(false);

    useEffect(() => {
        if (loaded) {
            SplashScreen.hideAsync();
        }
    }, [loaded]);

    const refreshData = () => {
        readSteps().then(data => {
            setStepData(data);
        });
    }

    const refreshTaskInfo = () => {
        load<TaskInfo>("taskInfo").then(setTaskInfo);
    }

    useEffect(() => {
        refreshData();
        load<Options>("options").then(setOptions);
        refreshTaskInfo();
        status().then(status => {
            setTaskStatus(status);
        });
    }, []);

    if (!loaded) {
        return null;
    }

    const sourceMap = stepData
        ?.map(dp => ({
            name: dp.metadata?.dataOrigin || "unknown",
            steps: dp.count
        }))
        .reduce((p, c) => {
            p.set(c.name, (p.get(c.name) || 0) + c.steps);
            return p;
        }, new Map<string, number>());
    const sources = Array.from(sourceMap?.keys() || []).map(key => ({
        app: key,
        steps: sourceMap?.get(key) || 0
    }));

    const steps = stepData?.reduce((p, c) => p + c.count, 0) || 0;
    const lastExecution = taskInfo ? new Date(taskInfo.lastExecution) : null;

    let taskStatusDisplay = <Text variant='bodyMedium'>loading...</Text>;
    switch(taskStatus) {
        case BackgroundFetch.STATUS_RESTRICTED:
            taskStatusDisplay = <Text variant='bodyMedium' style={{color: theme.colors.error}}>RESTRICTED</Text>;
            break;
        case BackgroundFetch.STATUS_DENIED:
            taskStatusDisplay = <Text variant='bodyMedium' style={{color: theme.colors.error}}>DENIED</Text>;
            break;
        case BackgroundFetch.STATUS_AVAILABLE:
            taskStatusDisplay = <Text variant='bodyMedium' style={{color: theme.colors.primary}}>AVAILABLE</Text>;
            break;
    }

    return (
        <View style={{
            backgroundColor: theme.colors.background,
            height: "100%"
        }}>
            <StatusBar backgroundColor={theme.colors.background}/>
            <Card style={{
                margin: 16,
            }}>
                <Card.Content style={{
                    display: "flex",
                    flexDirection: "column",
                    alignItems: "center"
                }}>
                    <Text variant='displaySmall'>{steps}</Text>
                    <ProgressBar progress={options?.target ? (steps / options.target) : 0} style={{width: 240, borderRadius: 16, height: 8, marginTop: 16}}/>
                </Card.Content>
            </Card>
            <Card style={{
                margin: 16,
                marginTop: 0
            }}>
                <Card.Content>
                    <Text variant='bodyLarge'>Sources</Text>
                    {sources === null ? 
                        <Text variant='bodyLarge' style={{textAlign: "center"}}>loading...</Text>
                    : sources.length <= 0 ?
                        <Text variant='bodyLarge' style={{textAlign: "center"}}>(no Data)</Text>
                    : sources.map(source => <View style={{flexDirection: "row", justifyContent: "space-between"}}>
                        <Text variant='bodyMedium'>{source.app}</Text>
                        <Text variant='bodyMedium'>{source.steps}</Text>
                    </View>)}
                </Card.Content>
            </Card>
            <Card style={{
                margin: 16,
                marginTop: 0
            }}>
                <Card.Content>
                    <Text variant='bodyLarge' style={{marginBottom: 16}}>Task Info</Text>
                    <View style={{
                        flexDirection: "row",
                        justifyContent: "space-between"
                    }}>
                        <Text variant='bodyMedium'>status:</Text>
                        {taskStatusDisplay}
                    </View>
                    <View style={{
                        flexDirection: "row",
                        justifyContent: "space-between"
                    }}>
                        <Text variant='bodyMedium'>last execution:</Text>
                        <Text variant='bodyMedium'>{lastExecution ? lastExecution > new Date(0) ? dateToString(lastExecution) : "never" : "loading..."}</Text>
                    </View>
                    <View style={{
                        flexDirection: "row",
                        justifyContent: "space-between"
                    }}>
                        <Text variant='bodyMedium'>steps added:</Text>
                        <Text variant='bodyMedium'>{taskInfo ? taskInfo.stepsAdded.toString() : "loading..."}</Text>
                    </View>
                </Card.Content>
            </Card>
            <Card style={{
                margin: 16,
                marginTop: 0
            }}>
                <Card.Content>
                    <Text variant='bodyLarge'>Options</Text>
                    {options && <>
                        <TextInput label="Target" keyboardType='numeric' value={options.target.toString()} style={{marginTop: 16}} onChangeText={e => {
                            const digits = e.replaceAll(/\D/g, "");
                            const number = digits.length > 0 ? Number(digits) : 0;
                            setOptions({ ...options, target: number });
                        }}></TextInput>
                        <View onTouchStart={(e) => {
                                setFromPickerVisible(true);
                            }}>
                            <TextInput label="Active From" value={options.from} readOnly></TextInput>
                        </View>
                        <TimePickerModal
                            visible={fromPickerVisible}
                            onDismiss={() => setFromPickerVisible(false)}
                            onConfirm={e => {
                                setOptions({
                                    ...options,
                                    from: `${e.hours.toString().padStart(2, "0")}:${e.minutes.toString().padStart(2, "0")}`
                                });
                                setFromPickerVisible(false);
                            }}
                            hours={Number(options.from.split(":")[0])}
                            minutes={Number(options.from.split(":")[1])}
                            use24HourClock
                        />
                        <View onTouchStart={(e) => {
                                setToPickerVisible(true);
                            }}>
                            <TextInput label="Active To" value={options.to} readOnly></TextInput>
                        </View>
                        <TimePickerModal
                            visible={toPickerVisible}
                            onDismiss={() => setToPickerVisible(false)}
                            onConfirm={e => {
                                setOptions({
                                    ...options,
                                    to: `${e.hours.toString().padStart(2, "0")}:${e.minutes.toString().padStart(2, "0")}`
                                });
                                setToPickerVisible(false);
                            }}
                            hours={Number(options.to.split(":")[0])}
                            minutes={Number(options.to.split(":")[1])}
                            use24HourClock
                        />
                        <Button onPress={() => save("options", options)} style={{marginTop: 16}}>Save</Button>
                    </>}
                </Card.Content>
            </Card>
        </View>
    );
}
