const { useEffect, useRef, useState } = React;

function formatElapsed(ms) {
    if (ms < 1000) {
        return `${ms} ms`;
    }

    const seconds = ms / 1000;
    if (seconds < 60) {
        return `${seconds.toFixed(2)} s`;
    }

    const minutes = seconds / 60;
    if (minutes < 60) {
        return `${minutes.toFixed(2)} min`;
    }

    const hours = minutes / 60;
    if (hours < 24) {
        return `${hours.toFixed(2)} h`;
    }

    return `${(hours / 24).toFixed(2)} d`;
}

function App() {
    const [digits, setDigits] = useState(300);
    const [delayMs, setDelayMs] = useState(35);
    const [tracks, setTracks] = useState({ single: [], concurrent: [] });
    const [meta, setMeta] = useState({ processors: 0, concurrentThreads: 0 });
    const [status, setStatus] = useState("ready");
    const [measuredElapsedMs, setMeasuredElapsedMs] = useState(0);
    const streamRef = useRef(null);
    const compareViewportRef = useRef(null);
    const playbackTimerRef = useRef(null);

    useEffect(() => {
        return () => stopStream();
    }, []);

    useEffect(() => {
        if (!compareViewportRef.current) {
            return;
        }

        compareViewportRef.current.scrollTo({
            left: compareViewportRef.current.scrollWidth,
            behavior: "smooth"
        });
    }, [tracks]);

    function stopStream() {
        if (streamRef.current) {
            streamRef.current.close();
            streamRef.current = null;
        }

        if (playbackTimerRef.current) {
            clearTimeout(playbackTimerRef.current);
            playbackTimerRef.current = null;
        }
    }

    function startStream() {
        stopStream();
        setTracks({ single: [], concurrent: [] });
        setMeta({ processors: 0, concurrentThreads: 0 });
        setMeasuredElapsedMs(0);
        setStatus("streaming");

        const source = new EventSource(`/api/pi/compare-stream?digits=${digits}&delayMs=${delayMs}`);
        streamRef.current = source;

        source.addEventListener("snapshot", event => {
            const payload = JSON.parse(event.data);
            const single = payload.singleThreadEvents ?? [];
            const concurrent = payload.concurrentEvents ?? [];

            setMeta(payload.metadata ?? { processors: 0, concurrentThreads: 0 });
            setMeasuredElapsedMs(Math.max(
                single.at(-1)?.elapsedMillis ?? 0,
                concurrent.at(-1)?.elapsedMillis ?? 0
            ));
            playTracks(single, concurrent, Math.max(0, Number(delayMs) || 0));
        });

        source.addEventListener("done", () => {
            if (streamRef.current) {
                streamRef.current.close();
                streamRef.current = null;
            }
        });

        source.onerror = () => {
            setStatus("stopped");
            stopStream();
        };
    }

    function playTracks(single, concurrent, delay) {
        const total = Math.max(single.length, concurrent.length);

        if (delay === 0) {
            setTracks({
                single: single.map(item => ({ ...item, row: "single" })),
                concurrent: concurrent.map(item => ({ ...item, row: "concurrent" }))
            });
            setStatus("completed");
            return;
        }

        let index = 0;
        const step = () => {
            setTracks(current => ({
                single: index < single.length
                    ? [...current.single, { ...single[index], row: "single" }]
                    : current.single,
                concurrent: index < concurrent.length
                    ? [...current.concurrent, { ...concurrent[index], row: "concurrent" }]
                    : current.concurrent
            }));

            index++;
            if (index >= total) {
                setStatus("completed");
                playbackTimerRef.current = null;
                return;
            }

            playbackTimerRef.current = setTimeout(step, delay);
        };

        step();
    }

    const totalElapsed = formatElapsed(measuredElapsedMs);
    const singleShown = tracks.single.length;
    const concurrentShown = tracks.concurrent.length;

    function renderDigitTile(title, item, extraClass = "") {
        const classes = ["digit-tile"];
        if (item.symbol === ".") {
            classes.push("dot");
        }
        if (extraClass) {
            classes.push(extraClass);
        }

        return (
            <div className={classes.join(" ")} key={`${title}-${item.index}`}>
                <div className="digit-time">{formatElapsed(item.elapsedMillis)}</div>
                <div className="digit-value">{item.symbol}</div>
            </div>
        );
    }

    function renderPrefixTile(title, items) {
        const prefixItems = items.slice(0, 2);
        if (prefixItems.length === 0) {
            return null;
        }

        const prefixValue = prefixItems.map(item => item.symbol).join("");
        const elapsedMillis = Math.max(...prefixItems.map(item => item.elapsedMillis ?? 0));

        return (
            <div className="digit-tile prefix-tile prefix-combined" key={`${title}-prefix`}>
                <div className="digit-time">{formatElapsed(elapsedMillis)}</div>
                <div className="digit-value">{prefixValue}</div>
            </div>
        );
    }

    const singleScrolling = tracks.single.slice(2);
    const concurrentScrolling = tracks.concurrent.slice(2);

    return (
        <main className="page-shell">
            <section className="hero-card">
                <div className="hero-copy">
                    <p className="eyebrow">Spring Boot + React</p>
                    <h1>Визуальное вычисление числа Pi</h1>
                    <p className="lead">
                        Две строки показывают однопоточное и многопоточное вычисление. Каждая цифра стоит строго под своей парой для удобного сравнения.
                    </p>
                    <p className="author-line">Автор Олег Чумин · tschumin.oleg@gmail.com</p>
                </div>

                <div className="controls-card">
                    <label>
                        <span>Цифр после запятой</span>
                        <input
                            type="number"
                            min="1"
                            max="2000"
                            value={digits}
                            onChange={event => setDigits(Number(event.target.value))}
                        />
                    </label>

                    <label>
                        <span>Пауза между цифрами, мс</span>
                        <input
                            type="number"
                            min="0"
                            max="500"
                            value={delayMs}
                            onChange={event => setDelayMs(Number(event.target.value))}
                        />
                    </label>

                    <div className="button-row">
                        <button className="primary" onClick={startStream}>Старт</button>
                        <button className="ghost" onClick={() => {
                            stopStream();
                            setStatus("stopped");
                        }}>Стоп</button>
                    </div>
                </div>
            </section>

            <section className="stats-grid">
                <article className="stat-card">
                    <span className="stat-label">Статус</span>
                    <strong>{status}</strong>
                </article>
                <article className="stat-card">
                    <span className="stat-label">Однопоточная строка</span>
                    <strong>{singleShown}</strong>
                </article>
                <article className="stat-card">
                    <span className="stat-label">Многопоточная строка</span>
                    <strong>{concurrentShown}</strong>
                </article>
                <article className="stat-card">
                    <span className="stat-label">Общее время</span>
                    <strong>{totalElapsed}</strong>
                </article>
                <article className="stat-card">
                    <span className="stat-label">Логических процессоров</span>
                    <strong>{meta.processors || "..."}</strong>
                </article>
                <article className="stat-card">
                    <span className="stat-label">Потоков в многопоточности</span>
                    <strong>{meta.concurrentThreads || "..."}</strong>
                </article>
            </section>

            <section className="stream-card">
                <div className="stream-header">
                    <div>
                        <p className="eyebrow">Сравнение режимов</p>
                        <h2>Общая шкала числа Pi</h2>
                    </div>
                </div>

                <div className="compare-rail">
                    {singleShown === 0 && concurrentShown === 0 ? (
                        <div className="empty-state">Нажмите «Старт», чтобы начать вычисление.</div>
                    ) : (
                        <div className="compare-layout">
                            <div className="compare-labels">
                                <div className="track-label-card">
                                    <div className="row-title">Однопоточность</div>
                                    <div className="row-subtitle">{`потоков 1 · процессоров ${meta.processors || "..."}`}</div>
                                </div>
                                <div className="track-label-card">
                                    <div className="row-title">Многопоточность</div>
                                    <div className="row-subtitle">{`потоков ${meta.concurrentThreads || "..."} · процессоров ${meta.processors || "..."}`}</div>
                                </div>
                            </div>

                            <div className="compare-prefixes">
                                <div className="digit-row prefix-row">
                                    {renderPrefixTile("single", tracks.single)}
                                </div>
                                <div className="digit-row prefix-row">
                                    {renderPrefixTile("concurrent", tracks.concurrent)}
                                </div>
                            </div>

                            <div className="compare-viewport" ref={compareViewportRef}>
                                <div className="compare-canvas">
                                    <div className="digit-row">
                                        {singleScrolling.map(item => renderDigitTile("single", item))}
                                    </div>
                                    <div className="digit-row">
                                        {concurrentScrolling.map(item => renderDigitTile("concurrent", item))}
                                    </div>
                                </div>
                            </div>
                        </div>
                    )}
                </div>
            </section>
        </main>
    );
}

ReactDOM.createRoot(document.getElementById("root")).render(<App />);
