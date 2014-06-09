--
-- PostgreSQL database dump
--

SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = off;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET escape_string_warning = off;

SET search_path = public, pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: profiling; Type: TABLE; Schema: public; Owner: webstorm; Tablespace: 
--

CREATE TABLE profiling (
    id integer NOT NULL,
    deployment_id character varying,
    bolt_type character varying,
    tuple_uuid character varying,
    hostname character varying,
    "timestamp" timestamp without time zone DEFAULT now() NOT NULL,
    "time" integer
);


ALTER TABLE public.profiling OWNER TO webstorm;

--
-- Name: profiling_id_seq; Type: SEQUENCE; Schema: public; Owner: webstorm
--

CREATE SEQUENCE profiling_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


ALTER TABLE public.profiling_id_seq OWNER TO webstorm;

--
-- Name: profiling_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: webstorm
--

ALTER SEQUENCE profiling_id_seq OWNED BY profiling.id;


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: webstorm
--

ALTER TABLE ONLY profiling ALTER COLUMN id SET DEFAULT nextval('profiling_id_seq'::regclass);


--
-- Name: profiling_deployment_id_idx; Type: INDEX; Schema: public; Owner: webstorm; Tablespace: 
--

CREATE INDEX profiling_deployment_id_idx ON profiling USING btree (deployment_id);


--
-- Name: profiling_hostname_idx; Type: INDEX; Schema: public; Owner: webstorm; Tablespace: 
--

CREATE INDEX profiling_hostname_idx ON profiling USING btree (hostname);


--
-- Name: profiling_timestamp_idx; Type: INDEX; Schema: public; Owner: webstorm; Tablespace: 
--

CREATE INDEX profiling_timestamp_idx ON profiling USING btree ("timestamp");


--
-- Name: profiling_tuple_uuid_idx; Type: INDEX; Schema: public; Owner: webstorm; Tablespace: 
--

CREATE INDEX profiling_tuple_uuid_idx ON profiling USING btree (tuple_uuid);


--
-- Name: public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;


--
-- PostgreSQL database dump complete
--

