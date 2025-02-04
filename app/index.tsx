import * as SplashScreen from 'expo-splash-screen';
import { DarkTheme, DefaultTheme, ThemeProvider } from '@react-navigation/native';
import { useFonts } from 'expo-font';
import { useEffect, useState } from 'react';
import { useColorScheme } from '@/hooks/useColorScheme';
import { RecordResult } from "react-native-health-connect";
import { readSteps, writeSteps } from "@/api/health";
import { ThemedView } from "@/components/ThemedView";
import { ThemedText } from "@/components/ThemedText";
import { Button, TextInput } from 'react-native';

SplashScreen.preventAutoHideAsync();

export default function Index() {
  const colorScheme = useColorScheme();
  const [loaded] = useFonts({
    SpaceMono: require('../assets/fonts/SpaceMono-Regular.ttf'),
  });

  const [stepData, setStepData] = useState<RecordResult<"Steps">[] | null>(null);

  const [from, setFrom] = useState(new Date(new Date().getTime() - 1000 * 60 * 10));
  const [to, setTo] = useState(new Date(new Date().getTime() - 1000 * 60 * 10));

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

  useEffect(() => {
    refreshData();
  }, []);

  const addSteps = async () => {
    const sorted = stepData?.sort((a, b) => new Date(b.endTime).getTime() - new Date(a.endTime).getTime())
    const from = sorted?.length ? new Date(sorted[0].endTime) : new Date(new Date().getTime() - 1000 * 60 * 10);
    const to = new Date();
    const count = (to.getTime() - from.getTime()) / 1000;
    await writeSteps(from, to, count);
  }

  if (!loaded) {
    return null;
  }

  return (
    <ThemeProvider value={colorScheme === 'dark' ? DarkTheme : DefaultTheme}>
      <ThemedView
        style={{
          flex: 1,
          justifyContent: "center",
          alignItems: "center",
        }}
      >
        <ThemedText>{JSON.stringify(stepData)}</ThemedText>
        <Button title="add steps" onPress={addSteps}/>
      </ThemedView>
    </ThemeProvider>
  );
}
