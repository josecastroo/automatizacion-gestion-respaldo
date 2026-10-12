import { useEffect, useState } from 'react';
import { Download, FileText, CheckCircle2, XCircle, AlertTriangle, Ban, Clock, Search, RefreshCw } from 'lucide-react';

export default function History() {
    const [executions, setExecutions] = useState([]);
    const [loading, setLoading] = useState(true);
    const [searchTerm, setSearchTerm] = useState('');
    const [statusFilter, setStatusFilter] = useState('ALL');

    const fetchExecutions = () => {
        setLoading(true);
        fetch('http://localhost:8080/api/executions?size=50')
            .then((res) => res.json())
            .then((data) => {
                setExecutions(data.content || data);
                setLoading(false);
            })
            .catch((err) => {
                console.error("Error al cargar historial:", err);
                setLoading(false);
            });
    };

    useEffect(() => {
        fetchExecutions();
    }, []);

    const handleExport = (format) => {
        window.open(`http://localhost:8080/api/executions/export/${format}`, '_blank');
    };

    const getStatusBadge = (status) => {
        switch (status) {
            case 'EXITOSO':
                return <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-green-50 text-green-700 border border-green-100"><CheckCircle2 size={14}/> Exitoso</span>;
            case 'ERROR':
                return <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-red-50 text-red-700 border border-red-100"><XCircle size={14}/> Error</span>;
            case 'ADVERTENCIA':
                return <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-orange-50 text-orange-700 border border-orange-100"><AlertTriangle size={14}/> Advertencia</span>;
            case 'OMITIDO':
                return <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-gray-100 text-gray-700 border border-gray-200"><Ban size={14}/> Omitido</span>;
            case 'EN_EJECUCION':
                return <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-blue-50 text-blue-700 border border-blue-100"><Clock size={14} className="animate-pulse"/> En progreso</span>;
            default:
                return <span>{status}</span>;
        }
    };

    const formatDate = (dateString) => {
        if (!dateString) return '---';
        const date = new Date(dateString);
        return date.toLocaleString('es-CR', {
            day: '2-digit', month: '2-digit', year: 'numeric',
            hour: '2-digit', minute: '2-digit'
        });
    };

    // Filtrado por buscador y estado
    const filteredExecutions = executions.filter((exec) => {
        const matchesSearch =
            (exec.strategyName && exec.strategyName.toLowerCase().includes(searchTerm.toLowerCase())) ||
            (exec.databaseName && exec.databaseName.toLowerCase().includes(searchTerm.toLowerCase()));

        const matchesStatus = statusFilter === 'ALL' || exec.status === statusFilter;

        return matchesSearch && matchesStatus;
    });

    return (
        <div className="max-w-6xl mx-auto space-y-8 animate-in fade-in duration-500">
            <div className="flex justify-between items-end">
                <div>
                    <h2 className="text-3xl font-semibold tracking-tight text-gray-900">Historial de Ejecuciones</h2>
                    <p className="text-gray-500 mt-2">Evidencia y auditoría de todos los respaldos realizados.</p>
                </div>
                <div className="flex gap-3">
                    <button onClick={fetchExecutions} className="flex items-center gap-2 bg-white border border-gray-200 hover:bg-gray-50 text-gray-700 px-3 py-2.5 rounded-xl font-medium transition-colors shadow-sm" title="Actualizar datos">
                        <RefreshCw size={18} className={loading ? "animate-spin text-blue-600" : "text-gray-500"} />
                    </button>
                    <button onClick={() => handleExport('csv')} className="flex items-center gap-2 bg-white border border-gray-200 hover:bg-gray-50 text-gray-700 px-4 py-2.5 rounded-xl font-medium transition-colors shadow-sm">
                        <FileText size={18} className="text-gray-500" />
                        CSV
                    </button>
                    <button onClick={() => handleExport('pdf')} className="flex items-center gap-2 bg-white border border-gray-200 hover:bg-gray-50 text-gray-700 px-4 py-2.5 rounded-xl font-medium transition-colors shadow-sm">
                        <Download size={18} className="text-red-500" />
                        PDF
                    </button>
                </div>
            </div>

            {/* Barra de Buscador y Filtros */}
            <div className="bg-white p-4 rounded-2xl shadow-sm border border-gray-100 flex flex-col sm:flex-row gap-4 justify-between items-center">
                <div className="relative w-full sm:w-80">
                    <Search className="absolute left-3.5 top-3 text-gray-400" size={18} />
                    <input
                        type="text"
                        placeholder="Buscar por estrategia o base de datos..."
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                        className="w-full pl-10 pr-4 py-2 bg-gray-50 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 transition-all"
                    />
                </div>

                <div className="flex gap-2 w-full sm:w-auto overflow-x-auto pb-1 sm:pb-0">
                    {['ALL', 'EXITOSO', 'ERROR', 'ADVERTENCIA', 'OMITIDO'].map((status) => (
                        <button
                            key={status}
                            onClick={() => setStatusFilter(status)}
                            className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all whitespace-nowrap ${
                                statusFilter === status
                                    ? 'bg-blue-600 text-white shadow-sm'
                                    : 'bg-gray-50 text-gray-600 hover:bg-gray-100 border border-gray-200'
                            }`}
                        >
                            {status === 'ALL' ? 'Todos' : status}
                        </button>
                    ))}
                </div>
            </div>

            <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
                {loading ? (
                    <div className="flex justify-center py-16">
                        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
                    </div>
                ) : filteredExecutions.length === 0 ? (
                    <div className="text-center py-16 text-gray-500">No se encontraron registros en el historial con los filtros actuales.</div>
                ) : (
                    <table className="w-full text-left border-collapse">
                        <thead>
                        <tr className="bg-gray-50/50 border-b border-gray-100">
                            <th className="px-6 py-4 text-sm font-medium text-gray-500">Fecha de Inicio</th>
                            <th className="px-6 py-4 text-sm font-medium text-gray-500">Estrategia</th>
                            <th className="px-6 py-4 text-sm font-medium text-gray-500">Tipo</th>
                            <th className="px-6 py-4 text-sm font-medium text-gray-500">Origen</th>
                            <th className="px-6 py-4 text-sm font-medium text-gray-500">Duración</th>
                            <th className="px-6 py-4 text-sm font-medium text-gray-500">Estado</th>
                        </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-100">
                        {filteredExecutions.map((exec) => (
                            <tr key={exec.id} className="hover:bg-gray-50/50 transition-colors">
                                <td className="px-6 py-4 font-medium text-gray-900">{formatDate(exec.startTime)}</td>
                                <td className="px-6 py-4">
                                    <div className="font-medium text-gray-900">{exec.strategyName}</div>
                                    <div className="text-sm text-gray-500">{exec.databaseName}</div>
                                </td>
                                <td className="px-6 py-4 text-gray-700">{exec.backupType}</td>
                                <td className="px-6 py-4 text-gray-700">{exec.origin}</td>
                                <td className="px-6 py-4 text-gray-700">{exec.durationSeconds ? `${exec.durationSeconds} seg` : '---'}</td>
                                <td className="px-6 py-4">{getStatusBadge(exec.status)}</td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                )}
            </div>
        </div>
    );
}