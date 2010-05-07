/*
 * (c) Copyright 2008, 2009 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */

package tdb.cmdline;

import java.io.ByteArrayInputStream ;
import java.io.IOException ;
import java.io.InputStream ;
import java.util.Properties ;

import org.apache.log4j.Level ;
import org.apache.log4j.LogManager ;
import org.apache.log4j.PropertyConfigurator ;
import org.openjena.atlas.lib.StrUtils ;
import org.openjena.atlas.logging.Log ;
import arq.cmd.CmdException ;
import arq.cmdline.ArgDecl;
import arq.cmdline.CmdARQ;
import arq.cmdline.ModSymbol;

import com.hp.hpl.jena.Jena;
import com.hp.hpl.jena.query.ARQ;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.riot.RIOT ;
import com.hp.hpl.jena.sparql.util.StringUtils ;
import com.hp.hpl.jena.sparql.util.Utils;
import com.hp.hpl.jena.tdb.TDB;
import com.hp.hpl.jena.tdb.base.file.Location;
import com.hp.hpl.jena.tdb.store.DatasetGraphTDB;
import com.hp.hpl.jena.tdb.store.GraphTDB;
import com.hp.hpl.jena.tdb.sys.SystemTDB;

public abstract class CmdTDB extends CmdARQ
{
    private static final ArgDecl argNamedGraph       = new ArgDecl(ArgDecl.HasValue, "graph") ;
    protected final ModTDBDataset tdbDatasetAssembler = new ModTDBDataset() ;

    private static final String log4Jsetup = StringUtils.join("\n",
                                                        "## Loader - plain out for commands output.",
                                                        "log4j.appender.tdbcmd=org.apache.log4j.ConsoleAppender",
                                                        "log4j.appender.tdbcmd.target=System.err",
                                                        "log4j.appender.tdbcmd.layout=org.apache.log4j.PatternLayout",
                                                        "log4j.appender.tdbcmd.layout.ConversionPattern=%m%n",
                                                        "## Loader output",
                                                        "log4j.additivity."+TDB.logLoaderName+"=false",
                                                        "log4j.logger."+TDB.logLoaderName+"=ALL, tdbcmd",
                                                        "## Parser output",
                                                        "log4j.additivity."+RIOT.riotLoggerName+"=false",
                                                        "log4j.logger."+RIOT.riotLoggerName+"=INFO, tdbcmd ",
                                                        ""
                                                          ) ;
    private static boolean loggingInitialized = false ;
    
    protected String graphName = null ;
    
    protected CmdTDB(String[] argv)
    {
        super(argv) ;
        init() ;
        super.add(argNamedGraph, "--graph=IRI", "Act on a named graph") ;
        super.addModule(tdbDatasetAssembler) ;
        super.modVersion.addClass(Jena.class) ;
        super.modVersion.addClass(ARQ.class) ;
        super.modVersion.addClass(TDB.class) ;
    }
    
    public static void init()
    {
        setLogging() ;
        // This sets context based on system properties.
        // ModSymbol can then override. 
        TDB.init() ;
        ModSymbol.addPrefixMapping(SystemTDB.tdbSymbolPrefix, SystemTDB.symbolNamespace) ;
    }
    
    public static synchronized void setLogging()
    {
        if ( loggingInitialized )
            return ;
        // attempt once.
        loggingInitialized = true ;
        
        String x = System.getProperty("log4j.configuration") ; 
        
        //if ( ! Log.setLog4j() ) 
        //if ( System.getProperty("log4j.configuration") == null )
        {
            // Turn off optimizer warning.
            // Use a plain logger for output. 
            Properties p = new Properties() ;
           
            InputStream in = new ByteArrayInputStream(StrUtils.asUTF8bytes(log4Jsetup)) ;
            try { p.load(in) ; } catch (IOException ex) {}
            PropertyConfigurator.configure(p) ;
            
            LogManager.getLogger(TDB.logInfoName).setLevel(Level.ERROR) ;
            System.setProperty("log4j.configuration", "set") ;
        }
    }

    @Override
    protected void processModulesAndArgs()
    {
        super.processModulesAndArgs() ;
        if ( contains(argNamedGraph) )
            graphName = getValue(argNamedGraph) ; 
    }
    
    protected Model getModel()
    {
        Dataset ds = tdbDatasetAssembler.getDataset() ;
        
        if ( graphName != null )
        {
            Model m = ds.getNamedModel(graphName) ;
            if ( m == null )
                throw new CmdException("No such named graph (is this a TDB dataset?)") ;
            return m ;
        }
        else
            return ds.getDefaultModel() ;
    }
    
    protected Location getLocation()
    {
        return tdbDatasetAssembler.getLocation() ;
    }
    
    protected GraphTDB getGraph()
    {
        if ( graphName != null )
            return (GraphTDB)tdbDatasetAssembler.getDataset().getNamedModel(graphName).getGraph() ;
        else
            return (GraphTDB)tdbDatasetAssembler.getDataset().getDefaultModel().getGraph() ;
    }
    
    protected DatasetGraphTDB getDatasetGraph()
    {
        return (DatasetGraphTDB)getDataset().asDatasetGraph() ;
    }

    protected Dataset getDataset()
    {
        return tdbDatasetAssembler.getDataset() ;
    }
    
    @Override
    protected String getCommandName()
    {
        return Utils.className(this) ;
    }
    
}

/*
 * (c) Copyright 2008, 2009 Hewlett-Packard Development Company, LP
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */