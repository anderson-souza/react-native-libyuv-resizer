import { useState } from 'react';
import {
  ActivityIndicator,
  Button,
  Image,
  Modal,
  Pressable,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { launchImageLibrary } from 'react-native-image-picker';
import { resize } from 'react-native-libyuv-resizer';
import type { RotationAngle } from 'react-native-libyuv-resizer';

type ResizeResult = {
  outputPath: string;
  inputSize: { width: number; height: number };
  elapsedMs: number;
  label: string;
};

type Mode = {
  label: string;
  width: number;
  height: number;
  rotation?: RotationAngle;
};

const MODES: Mode[] = [
  { label: 'Resize only (2592×1944)', width: 1944, height: 2592 },
  {
    label: 'Resize + Rotate 90° (1944×2592)',
    width: 1944,
    height: 2592,
    rotation: 90,
  },
  {
    label: 'Resize + Rotate 180° (2592×1944)',
    width: 2592,
    height: 1944,
    rotation: 180,
  },
  {
    label: 'Resize + Rotate 270° (1944×2592)',
    width: 1944,
    height: 2592,
    rotation: 270,
  },
  {
    label: 'Resize + Rotate -90° → 270° (1944×2592)',
    width: 1944,
    height: 2592,
    rotation: -90,
  },
];

export default function App() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [results, setResults] = useState<ResizeResult[]>([]);
  const [fullscreenUri, setFullscreenUri] = useState<string | null>(null);

  async function pickAndResize() {
    setError(null);
    setResults([]);

    const response = await launchImageLibrary({
      mediaType: 'photo',
      includeBase64: false,
    });
    if (response.didCancel || !response.assets?.[0]?.uri) return;

    const asset = response.assets[0];
    const filePath = asset.uri!.replace('file://', '');

    setLoading(true);
    try {
      const outputs: ResizeResult[] = [];
      for (const mode of MODES) {
        const start = Date.now();
        const nativeResult = await resize(
          filePath,
          mode.width,
          mode.height,
          95,
          {
            rotation: mode.rotation,
            mode: 'cover',
            filterMode: 'box',
            keepMeta: false,
          }
        );
        outputs.push({
          outputPath: nativeResult.path,
          inputSize: { width: asset.width ?? 0, height: asset.height ?? 0 },
          elapsedMs: Date.now() - start,
          label: mode.label,
        });
      }
      setResults(outputs);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setLoading(false);
    }
  }

  return (
    <>
      <ScrollView contentContainerStyle={styles.container}>
        <Text style={styles.title}>libyuv Resizer + Rotate</Text>
        <Button title="Pick image from gallery" onPress={pickAndResize} />

        {loading && <ActivityIndicator style={styles.gap} size="large" />}

        {error && <Text style={styles.error}>{error}</Text>}

        {results.map((r) => (
          <View key={r.label} style={styles.gap}>
            <Text style={styles.sectionTitle}>{r.label}</Text>
            <Text style={styles.label}>
              Input: {r.inputSize.width}×{r.inputSize.height}
            </Text>
            <Text style={styles.label}>Time: {r.elapsedMs}ms</Text>
            <Pressable
              onPress={() => setFullscreenUri(`file://${r.outputPath}`)}
            >
              <Image
                source={{ uri: `file://${r.outputPath}` }}
                style={styles.preview}
                resizeMode="contain"
              />
            </Pressable>
          </View>
        ))}
      </ScrollView>

      <Modal visible={fullscreenUri !== null} transparent animationType="fade">
        <SafeAreaView style={styles.modalContainer}>
          <Pressable
            style={styles.closeButton}
            onPress={() => setFullscreenUri(null)}
          >
            <Text style={styles.closeText}>✕</Text>
          </Pressable>
          {fullscreenUri && (
            <Image
              source={{ uri: fullscreenUri }}
              style={styles.fullscreenImage}
              resizeMode="contain"
            />
          )}
        </SafeAreaView>
      </Modal>
    </>
  );
}

const styles = StyleSheet.create({
  container: {
    flexGrow: 1,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 24,
  },
  title: {
    fontSize: 20,
    fontWeight: 'bold',
    marginBottom: 24,
  },
  sectionTitle: {
    fontSize: 14,
    fontWeight: '600',
    marginBottom: 4,
    textAlign: 'center',
  },
  gap: {
    marginTop: 24,
    alignItems: 'center',
  },
  label: {
    fontSize: 13,
    marginBottom: 4,
    textAlign: 'center',
  },
  error: {
    marginTop: 16,
    color: 'red',
    textAlign: 'center',
  },
  preview: {
    marginTop: 12,
    width: 320,
    height: 320,
    borderWidth: 1,
    borderColor: '#ccc',
  },
  modalContainer: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.92)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  fullscreenImage: {
    width: '100%',
    height: '100%',
  },
  closeButton: {
    position: 'absolute',
    top: 16,
    right: 16,
    zIndex: 1,
    backgroundColor: 'rgba(255,255,255,0.2)',
    borderRadius: 20,
    width: 40,
    height: 40,
    justifyContent: 'center',
    alignItems: 'center',
  },
  closeText: {
    color: '#fff',
    fontSize: 18,
    fontWeight: 'bold',
  },
});
