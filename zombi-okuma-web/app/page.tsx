"use client";

import { useState } from "react";
import Game from "@/components/Game";
import Menu from "@/components/Menu";

export default function Home() {
  const [level, setLevel] = useState<number | null>(null);

  if (level === null) return <Menu onPlay={setLevel} />;
  return (
    <Game
      key={level}
      levelIndex={level}
      onExit={() => setLevel(null)}
      onNextLevel={() => setLevel(level + 1)}
    />
  );
}
