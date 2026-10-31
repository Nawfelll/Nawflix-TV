// Original Nawflix UI cues. Run with Node.js; no downloaded samples or packages.
const fs = require('fs');
const path = require('path');

const sampleRate = 44100;
const outDir = path.join(__dirname, '..', 'app', 'src', 'main', 'res', 'raw');
let seed = 17031;
const noise = () => {
  seed = (1664525 * seed + 1013904223) >>> 0;
  return (seed / 0xffffffff) * 2 - 1;
};
const tau = Math.PI * 2;
const decay = (t, rate) => Math.exp(-t * rate);
const attack = (t, seconds) => Math.min(1, t / seconds);

function render(seconds, voice) {
  const samples = new Float32Array(Math.ceil(seconds * sampleRate));
  for (let i = 0; i < samples.length; i++) samples[i] = voice(i / sampleRate);
  return samples;
}

function save(name, samples, peak) {
  let max = 0;
  for (const sample of samples) max = Math.max(max, Math.abs(sample));
  const scale = peak / Math.max(max, 1e-9);
  const wav = Buffer.alloc(44 + samples.length * 2);
  wav.write('RIFF', 0);
  wav.writeUInt32LE(wav.length - 8, 4);
  wav.write('WAVEfmt ', 8);
  wav.writeUInt32LE(16, 16);
  wav.writeUInt16LE(1, 20);
  wav.writeUInt16LE(1, 22);
  wav.writeUInt32LE(sampleRate, 24);
  wav.writeUInt32LE(sampleRate * 2, 28);
  wav.writeUInt16LE(2, 32);
  wav.writeUInt16LE(16, 34);
  wav.write('data', 36);
  wav.writeUInt32LE(samples.length * 2, 40);
  for (let i = 0; i < samples.length; i++) {
    const t = i / sampleRate;
    const fadeIn = attack(t, 0.002);
    const fadeOut = Math.min(1, (samples.length - i) / (sampleRate * 0.012));
    wav.writeInt16LE(Math.max(-32767, Math.min(32767, Math.round(samples[i] * scale * fadeIn * fadeOut * 32767))), 44 + 2 * i);
  }
  fs.writeFileSync(path.join(outDir, `${name}.wav`), wav);
  console.log(`${name}: ${(samples.length / sampleRate).toFixed(2)} s`);
}

// A soft rubbery pop with a short bass body. Quiet enough for repeated D-pad moves.
save('ui_move', render(0.085, t => {
  const pitch = 180 - 65 * Math.min(1, t / 0.04);
  return 0.8 * Math.sin(tau * pitch * t) * decay(t, 52)
    + 0.08 * noise() * decay(t, 105);
}), 0.25);

// Top navigation gets a slightly brighter pop and a gentle fifth above it.
save('ui_tab', render(0.15, t => {
  const body = Math.sin(tau * (225 - 45 * Math.min(1, t / 0.05)) * t) * decay(t, 30);
  const glass = Math.sin(tau * 675 * t) * decay(t, 38);
  return 0.85 * body + 0.19 * glass + 0.055 * noise() * decay(t, 110);
}), 0.34);

// Confirm: two compact, round notes over a warm low-frequency pulse.
save('ui_select', render(0.24, t => {
  const bass = Math.sin(tau * (110 - 32 * Math.min(1, t / 0.09)) * t) * decay(t, 18);
  const first = Math.sin(tau * 392 * t) * decay(t, 24);
  const secondT = t - 0.052;
  const second = secondT > 0 ? Math.sin(tau * 588 * secondT) * decay(secondT, 24) : 0;
  return 0.75 * bass + 0.23 * first + 0.2 * second;
}), 0.43);

// Back: a short falling cushion rather than a harsh click.
save('ui_back', render(0.14, t => {
  const pitch = 255 - 125 * Math.min(1, t / 0.085);
  return Math.sin(tau * pitch * t) * decay(t, 34)
    + 0.04 * noise() * decay(t, 90);
}), 0.29);

// Intro aligns with the 2.76 s motion: subtle sweep, then a mellow chord and bass pop.
save('nawflix_intro_chime', render(2.6, t => {
  const sweep = t < 1.04
    ? 0.13 * Math.sin(tau * (280 + 260 * t) * t) * Math.sin(Math.PI * t / 1.04) ** 2
    : 0;
  const hitT = t - 1.25;
  const bass = hitT >= 0 ? 0.58 * Math.sin(tau * (98 - 26 * Math.min(1, hitT / 0.22)) * hitT) * decay(hitT, 2.9) : 0;
  const chord = hitT >= 0 ? [329.63, 493.88, 659.25].reduce((sum, f, i) =>
    sum + (0.22 - i * 0.035) * Math.sin(tau * f * hitT) * decay(hitT, 3.1), 0) : 0;
  const air = hitT >= 0 ? 0.018 * noise() * decay(hitT, 7) : 0;
  return sweep + bass + chord + air;
}), 0.53);
