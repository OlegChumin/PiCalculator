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
    const [items, setItems] = useState([]);
    const [status, setStatus] = useState("ready");
    const [startedAt, setStartedAt] = useState(null);
    const [finishedAt, setFinishedAt] = useState(null);
    const streamRef = useRef(null);
    const railRef = useRef(null);

    useEffect(() => {
        return () => stopStream();
    }, []);

    useEffect(() => {
        if (!railRef.current) {
            return;
        }

        railRef.current.scrollTo({
            left: railRef.current.scrollWidth,
            behavior: "smooth"
        });
    }, [items]);

    function stopStream() {
        if (streamRef.current) {
            streamRef.current.close();
            streamRef.current = null;
        }
    }

    function startStream() {
        stopStream();
        setItems([]);
        setStatus("streaming");
        setStartedAt(Date.now());
        setFinishedAt(null);

        const source = new EventSource(`/api/pi/stream?digits=${digits}&delayMs=${delayMs}`);
        streamRef.current = source;

        source.addEventListener("digit", event => {
            const payload = JSON.parse(event.data);
            setItems(current => [...current, payload]);
        });

        source.addEventListener("done", () => {
            setStatus("completed");
            setFinishedAt(Date.now());
            stopStream();
        });

        source.onerror = () => {
            setStatus("stopped");
            setFinishedAt(Date.now());
            stopStream();
        };
    }

    const totalElapsed = startedAt ? formatElapsed((finishedAt ?? Date.now()) - startedAt) : "0 ms";

    return (
        <main className="page-shell">
            <section className="hero-card">
                <div className="hero-copy">
                    <p className="eyebrow">Spring Boot + React</p>
                    <h1>Визуальное вычисление числа Pi</h1>
                    <p className="lead">
                        Каждая цифра появляется отдельно. Над ней показывается, за какое время она была вычислена.
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
                            setFinishedAt(Date.now());
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
                    <span className="stat-label">Показано символов</span>
                    <strong>{items.length}</strong>
                </article>
                <article className="stat-card">
                    <span className="stat-label">Общее время</span>
                    <strong>{totalElapsed}</strong>
                </article>
            </section>

            <section className="stream-card">
                <div className="stream-header">
                    <div>
                        <p className="eyebrow">Поток цифр</p>
                        <h2>Число Pi на экране</h2>
                    </div>
                </div>

                <div className="digit-rail" ref={railRef}>
                    {items.length === 0 ? (
                        <div className="empty-state">Нажмите «Старт», чтобы начать вычисление.</div>
                    ) : (
                        items.map(item => (
                            <div className={`digit-tile ${item.symbol === "." ? "dot" : ""}`} key={item.index}>
                                <div className="digit-time">{formatElapsed(item.elapsedMillis)}</div>
                                <div className="digit-value">{item.symbol}</div>
                            </div>
                        ))
                    )}
                </div>
            </section>
        </main>
    );
}

ReactDOM.createRoot(document.getElementById("root")).render(<App />);
